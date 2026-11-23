package co.finanzas.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End to end through HTTP, Spring Security, the Mongo adapters and an embedded MongoDB. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegracionTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    private record Sesion(String token, Cookie refresh) {
    }

    private Sesion registrar(String correo) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana Pérez\",\"correo\":\"" + correo + "\",\"clave\":\"clave-segura-1\"}"))
                .andExpect(status().isCreated()).andReturn();
        return new Sesion(json.readTree(r.getResponse().getContentAsString()).get("accessToken").asText(),
                r.getResponse().getCookie("refresh_token"));
    }

    private JsonNode leer(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    private String bearer(Sesion s) {
        return "Bearer " + s.token();
    }

    private String idCategoria(Sesion s, String tipo, String nombre) throws Exception {
        JsonNode lista = leer(mvc.perform(get("/api/v1/categorias").param("tipo", tipo).header("Authorization", bearer(s)))
                .andExpect(status().isOk()).andReturn());
        for (JsonNode c : lista) {
            if (c.get("nombre").asText().equals(nombre)) {
                return c.get("id").asText();
            }
        }
        throw new AssertionError("Categoría no encontrada: " + nombre);
    }

    @Test
    void flujoCompletoDeUnUsuario() throws Exception {
        String correo = "ana+" + UUID.randomUUID() + "@correo.com";
        Sesion s = registrar(correo);
        assertThat(s.refresh()).isNotNull();
        assertThat(s.refresh().isHttpOnly()).isTrue();

        // RF-14: predefined categories exist
        String comida = idCategoria(s, "GASTO", "Alimentación");
        String salario = idCategoria(s, "INGRESO", "Salario");
        String hoy = LocalDate.now().toString();

        mvc.perform(post("/api/v1/movimientos").header("Authorization", bearer(s)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"INGRESO\",\"monto\":\"1000000\",\"fecha\":\"" + hoy + "\",\"idCategoria\":\"" + salario + "\",\"fuente\":\"Empresa\"}"))
                .andExpect(status().isCreated());
        String idGasto = leer(mvc.perform(post("/api/v1/movimientos").header("Authorization", bearer(s))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"GASTO\",\"monto\":\"250000.50\",\"fecha\":\"" + hoy + "\",\"idCategoria\":\"" + comida + "\",\"metodoPago\":\"TARJETA_DEBITO\",\"descripcion\":\"Mercado\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.monto").value(250000.50)).andReturn()).get("id").asText();

        // RN-03: income category for an expense -> 422
        mvc.perform(post("/api/v1/movimientos").header("Authorization", bearer(s)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"GASTO\",\"monto\":\"10\",\"fecha\":\"" + hoy + "\",\"idCategoria\":\"" + salario + "\"}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("CATEGORIA_TIPO"));

        // RF-21 history and RF-23/24 summary
        mvc.perform(get("/api/v1/movimientos").header("Authorization", bearer(s)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.elementos[0].categoria.nombre").exists());
        // History search over the whole range: description, income source and category name; the text is literal
        for (String[] caso : new String[][]{{"MERC", "1"}, {"empresa", "1"}, {"salar", "1"}, {"(", "0"}, {"nada", "0"}}) {
            mvc.perform(get("/api/v1/movimientos").param("texto", caso[0]).header("Authorization", bearer(s)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(Integer.parseInt(caso[1])));
        }
        mvc.perform(get("/api/v1/resumen").header("Authorization", bearer(s)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIngresos").value(1000000.00))
                .andExpect(jsonPath("$.totalGastos").value(250000.50))
                .andExpect(jsonPath("$.balance").value(749999.50))
                .andExpect(jsonPath("$.porCategoria[0].porcentaje").value(100.0));

        // RF-18/19 budget
        String periodo = YearMonth.now().toString();
        mvc.perform(put("/api/v1/presupuestos").header("Authorization", bearer(s)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCategoria\":\"" + comida + "\",\"periodo\":\"" + periodo + "\",\"limite\":\"300000\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ALERTA"));

        // RF-15..17 own category, duplicate, delete with reassignment
        String cafe = leer(mvc.perform(post("/api/v1/categorias").header("Authorization", bearer(s))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Café\",\"tipo\":\"GASTO\"}"))
                .andExpect(status().isCreated()).andReturn()).get("id").asText();
        mvc.perform(post("/api/v1/categorias").header("Authorization", bearer(s))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"café\",\"tipo\":\"GASTO\"}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/v1/movimientos/" + idGasto).header("Authorization", bearer(s)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"GASTO\",\"monto\":\"12000\",\"fecha\":\"" + hoy + "\",\"idCategoria\":\"" + cafe + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.categoria.nombre").value("Café"));
        mvc.perform(delete("/api/v1/categorias/" + cafe).header("Authorization", bearer(s)))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("DESTINO_OBLIGATORIO"));
        mvc.perform(delete("/api/v1/categorias/" + cafe).param("destino", comida).header("Authorization", bearer(s)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/movimientos/" + idGasto).header("Authorization", bearer(s)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.categoria.nombre").value("Alimentación"));

        // RNF-02: another user cannot see it
        Sesion otro = registrar("beto+" + UUID.randomUUID() + "@correo.com");
        mvc.perform(get("/api/v1/movimientos/" + idGasto).header("Authorization", bearer(otro))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/movimientos/" + idGasto).header("Authorization", bearer(otro))).andExpect(status().isNotFound());

        // RF-05 refresh with the HttpOnly cookie (rotation)
        // CSRF: the cookie alone (what another site could make the browser send) is rejected
        mvc.perform(post("/api/v1/auth/renovar").cookie(s.refresh()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("ORIGEN_NO_PERMITIDO"));
        MvcResult renovado = mvc.perform(post("/api/v1/auth/renovar").header("X-Requested-With", "XMLHttpRequest").cookie(s.refresh())).andExpect(status().isOk()).andReturn();
        // grace period: the rotated token works once more (lost response), then never again
        mvc.perform(post("/api/v1/auth/renovar").header("X-Requested-With", "XMLHttpRequest").cookie(s.refresh())).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/renovar").header("X-Requested-With", "XMLHttpRequest").cookie(s.refresh())).andExpect(status().isUnauthorized());
        Sesion s2 = new Sesion(leer(renovado).get("accessToken").asText(), renovado.getResponse().getCookie("refresh_token"));

        // RF-26/27 account
        mvc.perform(patch("/api/v1/cuenta").header("Authorization", bearer(s2)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana María\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Ana María"));
        mvc.perform(put("/api/v1/cuenta/contrasena").header("Authorization", bearer(s2)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"actual\":\"no-es-esta-1\",\"nueva\":\"nueva-clave-123\"}")).andExpect(status().isUnprocessableEntity());
        mvc.perform(put("/api/v1/cuenta/contrasena").header("Authorization", bearer(s2)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"actual\":\"clave-segura-1\",\"nueva\":\"nueva-clave-123\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/renovar").header("X-Requested-With", "XMLHttpRequest").cookie(s2.refresh())).andExpect(status().isUnauthorized());

        // RF-25 delete the account; the same e-mail can no longer sign in
        mvc.perform(delete("/api/v1/cuenta").header("Authorization", bearer(s2))).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"clave\":\"nueva-clave-123\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos"));
    }

    @Test
    void sinTokenResponde401EnFormatoProblemDetails() throws Exception {
        mvc.perform(get("/api/v1/movimientos")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        mvc.perform(get("/api/v1/movimientos").header("Authorization", "Bearer alterado")).andExpect(status().isUnauthorized());
    }

    @Test
    void registroDuplicadoYClaveCorta() throws Exception {
        String correo = "dup+" + UUID.randomUUID() + "@correo.com";
        registrar(correo);
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Otra\",\"correo\":\"" + correo.toUpperCase() + "\",\"clave\":\"clave-segura-1\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Este correo ya está registrado"));
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Otra\",\"correo\":\"x" + correo + "\",\"clave\":\"corta\"}"))
                .andExpect(status().isBadRequest());
    }
}
