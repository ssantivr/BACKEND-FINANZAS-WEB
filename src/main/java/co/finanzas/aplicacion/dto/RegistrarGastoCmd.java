package co.finanzas.aplicacion.dto;

import co.finanzas.dominio.tipos.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RegistrarGastoCmd(UUID idUsuario, BigDecimal monto, LocalDate fecha, String descripcion,
                                UUID idCategoria, MetodoPago metodoPago) {
}
