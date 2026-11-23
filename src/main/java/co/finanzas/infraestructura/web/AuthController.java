package co.finanzas.infraestructura.web;

import co.finanzas.aplicacion.dto.RegistroCmd;
import co.finanzas.aplicacion.dto.TokensDTO;
import co.finanzas.aplicacion.puertos.entrada.IAutenticacion;
import co.finanzas.dominio.excepciones.ReglaNegocioException;
import co.finanzas.infraestructura.web.CookieSesion.SesionResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    public record RegistroRequest(@NotBlank String nombre, @NotBlank String correo, @NotBlank String clave) {
    }

    public record CredencialesRequest(@NotBlank String correo, @NotBlank String clave) {
    }

    public record CorreoRequest(@NotBlank String correo) {
    }

    public record RestablecerRequest(@NotBlank String token, @NotBlank String nuevaClave) {
    }

    private final IAutenticacion autenticacion;
    private final CookieSesion cookie;

    public AuthController(IAutenticacion autenticacion, CookieSesion cookie) {
        this.autenticacion = autenticacion;
        this.cookie = cookie;
    }

    /** RF-01: creates the account and leaves the session started (HU-01). */
    @PostMapping("/registro")
    public ResponseEntity<SesionResponse> registrar(@Valid @RequestBody RegistroRequest r) {
        autenticacion.registrar(new RegistroCmd(r.nombre(), r.correo(), r.clave()));
        TokensDTO t = autenticacion.iniciarSesion(r.correo(), r.clave());
        return ResponseEntity.status(HttpStatus.CREATED).header(HttpHeaders.SET_COOKIE, cookie.crear(t))
                .body(SesionResponse.de(t));
    }

    @PostMapping("/login")
    public ResponseEntity<SesionResponse> iniciarSesion(@Valid @RequestBody CredencialesRequest r) {
        TokensDTO t = autenticacion.iniciarSesion(r.correo(), r.clave());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.crear(t)).body(SesionResponse.de(t));
    }

    /** Custom header that a cross-site form or link cannot send without a CORS preflight (which our CORS rejects). */
    static final String CABECERA_CSRF = "X-Requested-With";
    static final String VALOR_CSRF = "XMLHttpRequest";

    /**
     * CSRF defence for the two endpoints authenticated only by the refresh cookie: another site can make the browser
     * send the cookie, but not this header.
     */
    private static void exigirPeticionDeLaApp(String cabecera) {
        if (!VALOR_CSRF.equals(cabecera)) {
            throw new ReglaNegocioException("ORIGEN_NO_PERMITIDO",
                    "Falta la cabecera " + CABECERA_CSRF + ": " + VALOR_CSRF + " (protección CSRF)");
        }
    }

    @PostMapping("/renovar")
    public ResponseEntity<SesionResponse> renovar(@CookieValue(name = CookieSesion.NOMBRE, required = false) String refresh,
                                                  @RequestHeader(name = CABECERA_CSRF, required = false) String cabecera) {
        exigirPeticionDeLaApp(cabecera);
        TokensDTO t = autenticacion.renovarSesion(refresh);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.crear(t)).body(SesionResponse.de(t));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> cerrarSesion(@CookieValue(name = CookieSesion.NOMBRE, required = false) String refresh,
                                             @RequestHeader(name = CABECERA_CSRF, required = false) String cabecera) {
        exigirPeticionDeLaApp(cabecera);
        autenticacion.cerrarSesion(refresh);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.borrar()).build();
    }

    @PostMapping("/recuperacion")
    public ResponseEntity<Void> solicitarRecuperacion(@Valid @RequestBody CorreoRequest r) {
        autenticacion.solicitarRecuperacion(r.correo());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/restablecer")
    public ResponseEntity<Void> restablecer(@Valid @RequestBody RestablecerRequest r) {
        autenticacion.restablecerContrasena(r.token(), r.nuevaClave());
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.borrar()).build();
    }
}
