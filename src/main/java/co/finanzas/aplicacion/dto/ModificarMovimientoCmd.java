package co.finanzas.aplicacion.dto;

import co.finanzas.dominio.tipos.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** {@code metodoPago} applies to expenses and {@code fuente} to income; the other one is ignored. */
public record ModificarMovimientoCmd(BigDecimal monto, LocalDate fecha, String descripcion, UUID idCategoria,
                                     MetodoPago metodoPago, String fuente) {
}
