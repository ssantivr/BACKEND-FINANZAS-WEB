package co.finanzas.aplicacion.dto;

import co.finanzas.dominio.tipos.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Sum of one movement type on one day; input of the trend in the summary. */
public record TotalDiario(LocalDate fecha, TipoMovimiento tipo, BigDecimal total) {
}
