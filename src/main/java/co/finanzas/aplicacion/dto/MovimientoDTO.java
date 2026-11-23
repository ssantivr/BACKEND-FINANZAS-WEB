package co.finanzas.aplicacion.dto;

import co.finanzas.dominio.modelo.Gasto;
import co.finanzas.dominio.modelo.Ingreso;
import co.finanzas.dominio.modelo.Movimiento;
import co.finanzas.dominio.tipos.MetodoPago;
import co.finanzas.dominio.tipos.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MovimientoDTO(UUID id, TipoMovimiento tipo, BigDecimal monto, LocalDate fecha, String descripcion,
                            CategoriaDTO categoria, MetodoPago metodoPago, String fuente) {

    public static MovimientoDTO de(Movimiento m) {
        MetodoPago metodo = m instanceof Gasto g ? g.getMetodoPago() : null;
        String fuente = m instanceof Ingreso i ? i.getFuente() : null;
        return new MovimientoDTO(m.getId(), m.getTipo(), m.getMonto().getCantidad(), m.getFecha(), m.getDescripcion(),
                CategoriaDTO.de(m.getCategoria()), metodo, fuente);
    }
}
