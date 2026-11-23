package co.finanzas.dominio.valor;

import co.finanzas.dominio.excepciones.ReglaNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/** Money value object: exact decimal amount (scale 2, HALF_EVEN) plus currency. Never a double (RNF-05). */
public final class Dinero implements Comparable<Dinero> {

    public static final Currency COP = Currency.getInstance("COP");
    /** RN-01: upper bound of a single amount. */
    public static final BigDecimal MAXIMO = new BigDecimal("999999999999.99");

    private final BigDecimal cantidad;
    private final Currency moneda;

    public Dinero(BigDecimal cantidad, Currency moneda) {
        Objects.requireNonNull(cantidad, "cantidad");
        Objects.requireNonNull(moneda, "moneda");
        if (cantidad.stripTrailingZeros().scale() > 2) {
            throw new ReglaNegocioException("MONTO_DECIMALES", "El monto admite como máximo 2 decimales");
        }
        this.cantidad = cantidad.setScale(2, RoundingMode.HALF_EVEN);
        this.moneda = moneda;
    }

    public static Dinero de(BigDecimal cantidad) {
        return new Dinero(cantidad, COP);
    }

    public static Dinero cero(Currency moneda) {
        return new Dinero(BigDecimal.ZERO, moneda);
    }

    public Dinero sumar(Dinero otro) {
        exigirMismaMoneda(otro);
        return new Dinero(cantidad.add(otro.cantidad), moneda);
    }

    public Dinero restar(Dinero otro) {
        exigirMismaMoneda(otro);
        return new Dinero(cantidad.subtract(otro.cantidad), moneda);
    }

    /** Percentage of this amount over {@code total}, with one decimal. Zero when total is zero. */
    public BigDecimal porcentajeDe(Dinero total) {
        exigirMismaMoneda(total);
        if (total.cantidad.signum() == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_EVEN);
        }
        return cantidad.multiply(BigDecimal.valueOf(100)).divide(total.cantidad, 1, RoundingMode.HALF_EVEN);
    }

    public boolean esPositivo() {
        return cantidad.signum() > 0;
    }

    public boolean superaMaximo() {
        return cantidad.compareTo(MAXIMO) > 0;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public Currency getMoneda() {
        return moneda;
    }

    private void exigirMismaMoneda(Dinero otro) {
        if (!moneda.equals(otro.moneda)) {
            throw new ReglaNegocioException("MONEDA_DISTINTA", "No se pueden operar montos de monedas distintas");
        }
    }

    @Override
    public int compareTo(Dinero o) {
        exigirMismaMoneda(o);
        return cantidad.compareTo(o.cantidad);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Dinero d && cantidad.equals(d.cantidad) && moneda.equals(d.moneda);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cantidad, moneda);
    }

    @Override
    public String toString() {
        return cantidad.toPlainString() + " " + moneda.getCurrencyCode();
    }
}
