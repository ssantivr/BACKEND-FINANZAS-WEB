package co.finanzas.dominio.modelo;

import co.finanzas.dominio.excepciones.ReglaNegocioException;
import co.finanzas.dominio.tipos.TipoMovimiento;
import co.finanzas.dominio.valor.Dinero;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public abstract class Movimiento {

    protected final UUID id;
    protected final UUID idUsuario;
    protected Dinero monto;
    protected LocalDate fecha;
    protected String descripcion;
    protected Categoria categoria;
    protected final LocalDateTime fechaRegistro;

    protected Movimiento(UUID id, UUID idUsuario, Dinero monto, LocalDate fecha, String descripcion,
                         Categoria categoria, LocalDateTime fechaRegistro, LocalDate hoy) {
        this.id = Objects.requireNonNull(id);
        this.idUsuario = Objects.requireNonNull(idUsuario);
        this.fechaRegistro = Objects.requireNonNull(fechaRegistro);
        aplicar(monto, fecha, descripcion, categoria, hoy);
    }

    public abstract TipoMovimiento getTipo();

    /** +1 for income, -1 for expenses. */
    public int getSigno() {
        return getTipo() == TipoMovimiento.INGRESO ? 1 : -1;
    }

    public void modificar(Dinero monto, LocalDate fecha, String descripcion, Categoria categoria, LocalDate hoy) {
        aplicar(monto, fecha, descripcion, categoria, hoy);
    }

    public boolean perteneceA(UUID idUsuario) {
        return this.idUsuario.equals(idUsuario);
    }

    private void aplicar(Dinero monto, LocalDate fecha, String descripcion, Categoria categoria, LocalDate hoy) {
        this.monto = Objects.requireNonNull(monto, "monto");
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        this.descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
        this.categoria = Objects.requireNonNull(categoria, "categoria");
        validar(hoy);
    }

    /** RN-01, RN-02, RN-03 and RN-04. */
    protected void validar(LocalDate hoy) {
        if (!monto.esPositivo() || monto.superaMaximo()) {
            throw new ReglaNegocioException("MONTO_INVALIDO", "El monto debe ser mayor que 0 y no superar 999 999 999 999,99");
        }
        if (fecha.isAfter(hoy.plusDays(1))) {
            throw new ReglaNegocioException("FECHA_FUTURA", "La fecha no puede ser futura");
        }
        if (descripcion != null && descripcion.length() > 140) {
            throw new ReglaNegocioException("DESCRIPCION_LARGA", "La descripción admite como máximo 140 caracteres");
        }
        if (categoria.getTipo() != getTipo()) {
            throw new ReglaNegocioException("CATEGORIA_TIPO", "La categoría no corresponde al tipo de movimiento");
        }
        if (!categoria.estaDisponiblePara(idUsuario)) {
            throw new ReglaNegocioException("CATEGORIA_INVALIDA", "La categoría no existe");
        }
    }

    public UUID getId() { return id; }
    public UUID getIdUsuario() { return idUsuario; }
    public Dinero getMonto() { return monto; }
    public LocalDate getFecha() { return fecha; }
    public String getDescripcion() { return descripcion; }
    public Categoria getCategoria() { return categoria; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
}
