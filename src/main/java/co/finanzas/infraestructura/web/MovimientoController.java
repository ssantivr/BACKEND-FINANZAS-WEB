package co.finanzas.infraestructura.web;

import co.finanzas.aplicacion.dto.ModificarMovimientoCmd;
import co.finanzas.aplicacion.dto.MovimientoDTO;
import co.finanzas.aplicacion.dto.RegistrarGastoCmd;
import co.finanzas.aplicacion.dto.RegistrarIngresoCmd;
import co.finanzas.aplicacion.puertos.entrada.IGestionMovimientos;
import co.finanzas.dominio.tipos.MetodoPago;
import co.finanzas.dominio.tipos.TipoMovimiento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** RF-09, RF-10, RF-12, RF-13. Reading movements is in ConsultaController (Consultas component). */
@RestController
@RequestMapping("/api/v1/movimientos")
public class MovimientoController {

    public record MovimientoRequest(@NotNull TipoMovimiento tipo, @NotNull BigDecimal monto, @NotNull LocalDate fecha,
                                    String descripcion, @NotNull UUID idCategoria, MetodoPago metodoPago,
                                    String fuente) {
    }

    private final IGestionMovimientos gestion;

    public MovimientoController(IGestionMovimientos gestion) {
        this.gestion = gestion;
    }

    @PostMapping
    public ResponseEntity<MovimientoDTO> registrar(@AuthenticationPrincipal UUID idUsuario,
                                                   @Valid @RequestBody MovimientoRequest r) {
        MovimientoDTO creado = r.tipo() == TipoMovimiento.GASTO
                ? gestion.registrarGasto(new RegistrarGastoCmd(idUsuario, r.monto(), r.fecha(), r.descripcion(),
                r.idCategoria(), r.metodoPago() == null ? MetodoPago.EFECTIVO : r.metodoPago()))
                : gestion.registrarIngreso(new RegistrarIngresoCmd(idUsuario, r.monto(), r.fecha(), r.descripcion(),
                r.idCategoria(), r.fuente()));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    public MovimientoDTO modificar(@AuthenticationPrincipal UUID idUsuario, @PathVariable UUID id,
                                   @Valid @RequestBody MovimientoRequest r) {
        return gestion.modificar(idUsuario, id, new ModificarMovimientoCmd(r.monto(), r.fecha(), r.descripcion(),
                r.idCategoria(), r.metodoPago(), r.fuente()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal UUID idUsuario, @PathVariable UUID id) {
        gestion.eliminar(idUsuario, id);
        return ResponseEntity.noContent().build();
    }
}
