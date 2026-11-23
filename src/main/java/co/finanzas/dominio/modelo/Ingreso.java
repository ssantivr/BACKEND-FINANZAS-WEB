package co.finanzas.dominio.modelo;

import co.finanzas.dominio.tipos.TipoMovimiento;
import co.finanzas.dominio.valor.Dinero;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class Ingreso extends Movimiento {

    private String fuente;

    public Ingreso(UUID id, UUID idUsuario, Dinero monto, LocalDate fecha, String descripcion, Categoria categoria,
                   String fuente, LocalDateTime fechaRegistro, LocalDate hoy) {
        super(id, idUsuario, monto, fecha, descripcion, categoria, fechaRegistro, hoy);
        this.fuente = fuente == null || fuente.isBlank() ? null : fuente.trim();
    }

    @Override
    public TipoMovimiento getTipo() {
        return TipoMovimiento.INGRESO;
    }

    public void cambiarFuente(String fuente) {
        this.fuente = fuente == null || fuente.isBlank() ? null : fuente.trim();
    }

    public String getFuente() {
        return fuente;
    }
}
