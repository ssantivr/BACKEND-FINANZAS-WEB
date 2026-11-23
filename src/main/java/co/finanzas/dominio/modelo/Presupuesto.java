package co.finanzas.dominio.modelo;

import co.finanzas.dominio.excepciones.ReglaNegocioException;
import co.finanzas.dominio.tipos.EstadoPresupuesto;
import co.finanzas.dominio.tipos.TipoMovimiento;
import co.finanzas.dominio.valor.Dinero;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

public class Presupuesto {

    private static final BigDecimal UMBRAL_ALERTA = new BigDecimal("80");
    private static final BigDecimal UMBRAL_EXCEDIDO = new BigDecimal("100");

    private final UUID id;
    private final UUID idUsuario;
    private final Categoria categoria;
    private final YearMonth periodo;
    private Dinero limite;
    private boolean notificado;

    public Presupuesto(UUID id, UUID idUsuario, Categoria categoria, YearMonth periodo, Dinero limite, boolean notificado) {
        this.id = Objects.requireNonNull(id);
        this.idUsuario = Objects.requireNonNull(idUsuario);
        this.categoria = Objects.requireNonNull(categoria);
        this.periodo = Objects.requireNonNull(periodo);
        if (categoria.getTipo() != TipoMovimiento.GASTO) {   // RN-10
            throw new ReglaNegocioException("PRESUPUESTO_TIPO", "Solo se definen presupuestos para categorías de gasto");
        }
        this.limite = validarLimite(limite);
        this.notificado = notificado;
    }

    private static Dinero validarLimite(Dinero limite) {
        if (limite == null || !limite.esPositivo() || limite.superaMaximo()) {
            throw new ReglaNegocioException("LIMITE_INVALIDO", "El límite debe ser mayor que 0");
        }
        return limite;
    }

    /** RN-11: OK below 80 %, ALERTA from 80 % to 99 %, EXCEDIDO from 100 %. */
    public EstadoPresupuesto calcularEstado(Dinero consumo) {
        // Exact comparison (consumo * 100 vs limite * umbral): a rounded percentage would turn 79.99 % into 80 %.
        BigDecimal consumoPorCien = consumo.getCantidad().multiply(UMBRAL_EXCEDIDO);
        if (consumoPorCien.compareTo(limite.getCantidad().multiply(UMBRAL_EXCEDIDO)) >= 0) {
            return EstadoPresupuesto.EXCEDIDO;
        }
        return consumoPorCien.compareTo(limite.getCantidad().multiply(UMBRAL_ALERTA)) >= 0
                ? EstadoPresupuesto.ALERTA : EstadoPresupuesto.OK;
    }

    /** RN-12: only the first time it is exceeded in the month. */
    public boolean debeNotificar(Dinero consumo) {
        return !notificado && calcularEstado(consumo) == EstadoPresupuesto.EXCEDIDO;
    }

    public void marcarNotificado() {
        notificado = true;
    }

    /**
     * When spending goes back under the limit (a expense deleted or edited), the next excess alerts again.
     * @return true if the flag changed and the budget must be saved
     */
    public boolean reiniciarAvisoSiVolvioAlLimite(Dinero consumo) {
        if (notificado && calcularEstado(consumo) != EstadoPresupuesto.EXCEDIDO) {
            notificado = false;
            return true;
        }
        return false;
    }

    public void cambiarLimite(Dinero limite) {
        this.limite = validarLimite(limite);
        this.notificado = false;
    }

    public UUID getId() { return id; }
    public UUID getIdUsuario() { return idUsuario; }
    public Categoria getCategoria() { return categoria; }
    public YearMonth getPeriodo() { return periodo; }
    public Dinero getLimite() { return limite; }
    public boolean isNotificado() { return notificado; }
}
