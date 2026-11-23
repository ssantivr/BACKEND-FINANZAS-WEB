package co.finanzas.aplicacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ResumenFinanciero(LocalDate desde, LocalDate hasta, BigDecimal totalIngresos, BigDecimal totalGastos,
                                BigDecimal balance, List<TotalPorCategoria> porCategoria,
                                List<PresupuestoDTO> presupuestos,
                                /* Change against the previous range of the same length, in %; null when it had no data. */
                                BigDecimal variacionIngresos, BigDecimal variacionGastos,
                                BigDecimal promedioDiarioGastos, List<PuntoTendencia> tendencia) {
}
