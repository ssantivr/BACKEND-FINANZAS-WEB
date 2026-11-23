package co.finanzas.infraestructura.web;

import co.finanzas.aplicacion.dto.PresupuestoDTO;
import co.finanzas.aplicacion.puertos.entrada.IGestionPresupuestos;
import co.finanzas.dominio.excepciones.ReglaNegocioException;
import co.finanzas.dominio.valor.Dinero;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

/** RF-18, RF-19. {@code periodo} is "yyyy-MM". */
@RestController
@RequestMapping("/api/v1/presupuestos")
public class PresupuestoController {

    public record PresupuestoRequest(@NotNull UUID idCategoria, @NotBlank String periodo, @NotNull BigDecimal limite) {
    }

    private final IGestionPresupuestos gestion;
    private final Clock reloj;

    public PresupuestoController(IGestionPresupuestos gestion, Clock reloj) {
        this.gestion = gestion;
        this.reloj = reloj;
    }

    @PutMapping
    public PresupuestoDTO definir(@AuthenticationPrincipal UUID idUsuario, @Valid @RequestBody PresupuestoRequest r) {
        return gestion.definir(idUsuario, r.idCategoria(), periodo(r.periodo()), Dinero.de(r.limite()));
    }

    @GetMapping
    public List<PresupuestoDTO> listar(@AuthenticationPrincipal UUID idUsuario,
                                       @RequestParam(required = false) String periodo) {
        return gestion.listar(idUsuario, periodo == null ? YearMonth.now(reloj) : periodo(periodo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal UUID idUsuario, @PathVariable UUID id) {
        gestion.eliminar(idUsuario, id);
        return ResponseEntity.noContent().build();
    }

    private static YearMonth periodo(String texto) {
        try {
            return YearMonth.parse(texto);
        } catch (DateTimeParseException e) {
            throw new ReglaNegocioException("PERIODO_INVALIDO", "El periodo debe tener el formato AAAA-MM");
        }
    }
}
