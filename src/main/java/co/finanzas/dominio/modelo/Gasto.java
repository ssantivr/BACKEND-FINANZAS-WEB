package co.finanzas.dominio.modelo;

import co.finanzas.dominio.tipos.MetodoPago;
import co.finanzas.dominio.tipos.TipoMovimiento;
import co.finanzas.dominio.valor.Dinero;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Gasto extends Movimiento {

    private MetodoPago metodoPago;

    public Gasto(UUID id, UUID idUsuario, Dinero monto, LocalDate fecha, String descripcion, Categoria categoria,
                 MetodoPago metodoPago, LocalDateTime fechaRegistro, LocalDate hoy) {
        super(id, idUsuario, monto, fecha, descripcion, categoria, fechaRegistro, hoy);
        this.metodoPago = Objects.requireNonNull(metodoPago, "metodoPago");
    }

    @Override
    public TipoMovimiento getTipo() {
        return TipoMovimiento.GASTO;
    }

    public void cambiarMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = Objects.requireNonNull(metodoPago);
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }
}
