package co.finanzas.aplicacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One bar of the summary charts: a week (ranges up to 62 days) or a month (longer ranges). */
public record PuntoTendencia(String etiqueta, LocalDate desde, LocalDate hasta, BigDecimal ingresos, BigDecimal gastos) {
}
