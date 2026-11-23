package co.finanzas.aplicacion.dto;

import co.finanzas.dominio.modelo.Presupuesto;
import co.finanzas.dominio.tipos.EstadoPresupuesto;
import co.finanzas.dominio.valor.Dinero;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

public record PresupuestoDTO(UUID id, CategoriaDTO categoria, YearMonth periodo, BigDecimal limite,
                             BigDecimal consumo, BigDecimal porcentaje, EstadoPresupuesto estado) {

    public static PresupuestoDTO de(Presupuesto p, Dinero consumo) {
        return new PresupuestoDTO(p.getId(), CategoriaDTO.de(p.getCategoria()), p.getPeriodo(),
                p.getLimite().getCantidad(), consumo.getCantidad(), consumo.porcentajeDe(p.getLimite()),
                p.calcularEstado(consumo));
    }
}
