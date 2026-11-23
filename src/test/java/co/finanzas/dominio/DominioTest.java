package co.finanzas.dominio;

import co.finanzas.dominio.excepciones.ReglaNegocioException;
import co.finanzas.dominio.modelo.Categoria;
import co.finanzas.dominio.modelo.Gasto;
import co.finanzas.dominio.modelo.Presupuesto;
import co.finanzas.dominio.modelo.Usuario;
import co.finanzas.dominio.tipos.EstadoPresupuesto;
import co.finanzas.dominio.tipos.MetodoPago;
import co.finanzas.dominio.tipos.TipoMovimiento;
import co.finanzas.dominio.valor.Dinero;
import co.finanzas.dominio.valor.Email;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DominioTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 17);
    private static final UUID USUARIO = UUID.randomUUID();
    private static final Categoria COMIDA = new Categoria(UUID.randomUUID(), "Comida", TipoMovimiento.GASTO, null, null);
    private static final Categoria SALARIO = new Categoria(UUID.randomUUID(), "Salario", TipoMovimiento.INGRESO, null, null);

    private static Dinero d(String v) {
        return Dinero.de(new BigDecimal(v));
    }

    private static Gasto gasto(String monto, LocalDate fecha, Categoria c) {
        return new Gasto(UUID.randomUUID(), USUARIO, d(monto), fecha, "x", c, MetodoPago.EFECTIVO,
                LocalDateTime.of(2026, 11, 17, 10, 0), HOY);
    }

    @Test
    void dineroEsExactoYRechazaMasDeDosDecimales() {
        assertThat(d("0.10").sumar(d("0.20"))).isEqualTo(d("0.30"));
        assertThat(d("25").porcentajeDe(d("200"))).isEqualByComparingTo("12.5");
        assertThatThrownBy(() -> d("1.001")).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void emailSeNormalizaYValida() {
        assertThat(new Email("  Ana@Correo.COM ").getValor()).isEqualTo("ana@correo.com");
        assertThatThrownBy(() -> new Email("sin-arroba")).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void gastoAplicaReglasRn01Rn02Rn03() {
        assertThat(gasto("10.50", HOY, COMIDA).getSigno()).isEqualTo(-1);
        assertThatThrownBy(() -> gasto("0", HOY, COMIDA)).hasMessageContaining("mayor que 0");
        assertThatThrownBy(() -> gasto("10", HOY.plusDays(2), COMIDA)).hasMessageContaining("futura");
        assertThat(gasto("10", HOY.plusDays(1), COMIDA)).isNotNull();   // time-zone tolerance
        assertThatThrownBy(() -> gasto("10", HOY, SALARIO)).hasMessageContaining("no corresponde");
    }

    @Test
    void gastoNoAceptaCategoriaDeOtroUsuario() {
        Categoria ajena = new Categoria(UUID.randomUUID(), "Ajena", TipoMovimiento.GASTO, null, UUID.randomUUID());
        assertThatThrownBy(() -> gasto("10", HOY, ajena)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void usuarioSeBloqueaAlQuintoIntento() {
        Usuario u = new Usuario("Ana", new Email("ana@correo.com"), "hash", LocalDateTime.of(2026, 1, 1, 0, 0));
        LocalDateTime t = LocalDateTime.of(2026, 11, 17, 10, 0);
        for (int i = 0; i < 4; i++) {
            u.registrarIntentoFallido(t);
        }
        assertThat(u.estaBloqueado(t)).isFalse();
        u.registrarIntentoFallido(t);
        assertThat(u.estaBloqueado(t.plusMinutes(14))).isTrue();
        assertThat(u.estaBloqueado(t.plusMinutes(15))).isFalse();
    }

    @Test
    void usuarioEliminadoQuedaInactivo() {
        Usuario u = new Usuario("Ana", new Email("ana@correo.com"), "hash", LocalDateTime.of(2026, 1, 1, 0, 0));
        u.eliminar(LocalDateTime.of(2026, 11, 17, 10, 0));
        assertThat(u.estaActiva()).isFalse();
        assertThat(u.getEliminadaEn()).isNotNull();
    }

    @Test
    void presupuestoCalculaEstadoYNotificaUnaSolaVez() {
        Presupuesto p = new Presupuesto(UUID.randomUUID(), USUARIO, COMIDA, YearMonth.of(2026, 11), d("100"), false);
        assertThat(p.calcularEstado(d("79.99"))).isEqualTo(EstadoPresupuesto.OK);
        assertThat(p.calcularEstado(d("80"))).isEqualTo(EstadoPresupuesto.ALERTA);
        assertThat(p.calcularEstado(d("100"))).isEqualTo(EstadoPresupuesto.EXCEDIDO);
        assertThat(p.debeNotificar(d("120"))).isTrue();
        p.marcarNotificado();
        assertThat(p.debeNotificar(d("130"))).isFalse();
        p.cambiarLimite(d("110"));
        assertThat(p.debeNotificar(d("130"))).isTrue();
    }

    @Test
    void presupuestoSoloParaGastos() {
        assertThatThrownBy(() -> new Presupuesto(UUID.randomUUID(), USUARIO, SALARIO, YearMonth.of(2026, 11), d("10"), false))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void categoriaPredeterminadaNoSeRenombra() {
        assertThatThrownBy(() -> COMIDA.renombrar("Otra")).isInstanceOf(ReglaNegocioException.class);
        Categoria propia = Categoria.propia("Café", TipoMovimiento.GASTO, USUARIO);
        propia.renombrar("Cafetería");
        assertThat(propia.getNombre()).isEqualTo("Cafetería");
        assertThatThrownBy(() -> propia.renombrar("x".repeat(41))).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void categoriaSinColorRecibeUnoDeLaPaletaYValidaElFormato() {
        Categoria sinColor = Categoria.propia("Mascotas", TipoMovimiento.GASTO, USUARIO);
        assertThat(Categoria.PALETA).contains(sinColor.getColor());
        assertThat(Categoria.propia("Libros", TipoMovimiento.GASTO, "#A855F7", USUARIO).getColor()).isEqualTo("#a855f7");
        assertThatThrownBy(() -> Categoria.propia("Libros", TipoMovimiento.GASTO, "rojo", USUARIO))
                .hasMessage("El color debe tener el formato #RRGGBB");
    }

    @Test
    void presupuestoVuelveAAvisarSiElGastoBajaDelLimite() {
        Presupuesto p = new Presupuesto(UUID.randomUUID(), USUARIO, COMIDA, java.time.YearMonth.of(2026, 11), Dinero.de(new java.math.BigDecimal("100")), false);
        assertThat(p.debeNotificar(Dinero.de(new java.math.BigDecimal("120")))).isTrue();
        p.marcarNotificado();
        assertThat(p.reiniciarAvisoSiVolvioAlLimite(Dinero.de(new java.math.BigDecimal("120")))).isFalse();
        assertThat(p.reiniciarAvisoSiVolvioAlLimite(Dinero.de(new java.math.BigDecimal("90")))).isTrue();
        assertThat(p.debeNotificar(Dinero.de(new java.math.BigDecimal("130")))).isTrue();
    }
}
