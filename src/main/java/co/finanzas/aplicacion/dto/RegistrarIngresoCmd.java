package co.finanzas.aplicacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RegistrarIngresoCmd(UUID idUsuario, BigDecimal monto, LocalDate fecha, String descripcion,
                                  UUID idCategoria, String fuente) {
}
