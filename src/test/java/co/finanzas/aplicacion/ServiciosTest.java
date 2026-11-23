package co.finanzas.aplicacion;

import co.finanzas.aplicacion.dto.FiltroMovimientos;
import co.finanzas.aplicacion.dto.ModificarMovimientoCmd;
import co.finanzas.aplicacion.dto.MovimientoDTO;
import co.finanzas.aplicacion.dto.RegistrarGastoCmd;
import co.finanzas.aplicacion.dto.RegistrarIngresoCmd;
import co.finanzas.aplicacion.dto.RegistroCmd;
import co.finanzas.aplicacion.dto.ResumenFinanciero;
import co.finanzas.aplicacion.dto.TokensDTO;
import co.finanzas.aplicacion.dto.UsuarioDTO;
import co.finanzas.aplicacion.servicios.ServicioAutenticacion;
import co.finanzas.aplicacion.servicios.ServicioCategorias;
import co.finanzas.aplicacion.servicios.ServicioConsultaFinanciera;
import co.finanzas.aplicacion.servicios.ServicioCuenta;
import co.finanzas.aplicacion.servicios.ServicioMovimientos;
import co.finanzas.aplicacion.servicios.ServicioPresupuestos;
import co.finanzas.dominio.excepciones.RecursoNoEncontradoException;
import co.finanzas.dominio.excepciones.ReglaNegocioException;
import co.finanzas.dominio.modelo.Categoria;
import co.finanzas.dominio.tipos.MetodoPago;
import co.finanzas.dominio.tipos.TipoMovimiento;
import co.finanzas.dominio.valor.Dinero;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiciosTest {

    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-17T15:00:00Z"), ZoneOffset.UTC);
    private final LocalDate hoy = LocalDate.of(2026, 11, 17);

    private Memoria.Usuarios usuarios;
    private Memoria.Categorias categorias;
    private Memoria.Movimientos movimientos;
    private Memoria.Presupuestos presupuestos;
    private Memoria.Emisor emisor;
    private Memoria.Notificador notificador;
    private ServicioAutenticacion auth;
    private ServicioCuenta cuenta;
    private ServicioMovimientos movs;
    private ServicioCategorias cats;
    private ServicioPresupuestos press;
    private ServicioConsultaFinanciera consulta;
    private Categoria comida;
    private Categoria salario;

    @BeforeEach
    void preparar() {
        usuarios = new Memoria.Usuarios();
        categorias = new Memoria.Categorias();
        movimientos = new Memoria.Movimientos(categorias);
        presupuestos = new Memoria.Presupuestos();
        emisor = new Memoria.Emisor();
        notificador = new Memoria.Notificador();
        Memoria.Cifrador cifrador = new Memoria.Cifrador();
        auth = new ServicioAutenticacion(usuarios, cifrador, emisor, notificador, reloj, "http://front/restablecer");
        cuenta = new ServicioCuenta(usuarios, cifrador, emisor, reloj);
        press = new ServicioPresupuestos(presupuestos, movimientos, categorias, usuarios, notificador);
        movs = new ServicioMovimientos(movimientos, categorias, press, reloj);
        cats = new ServicioCategorias(categorias, movimientos, presupuestos);
        consulta = new ServicioConsultaFinanciera(movimientos, presupuestos, reloj);
        comida = new Categoria(UUID.randomUUID(), "Alimentación", TipoMovimiento.GASTO, null, null);
        salario = new Categoria(UUID.randomUUID(), "Salario", TipoMovimiento.INGRESO, null, null);
        categorias.guardar(comida);
        categorias.guardar(salario);
    }

    private UUID registrar(String correo) {
        return auth.registrar(new RegistroCmd("Ana Pérez", correo, "clave-segura-1")).id();
    }

    private MovimientoDTO gasto(UUID u, String monto, UUID cat) {
        return movs.registrarGasto(new RegistrarGastoCmd(u, new BigDecimal(monto), hoy, "almuerzo", cat, MetodoPago.EFECTIVO));
    }

    @Test
    void registroRechazaCorreoDuplicadoYClaveCorta() {
        registrar("ana@correo.com");
        assertThatThrownBy(() -> registrar("ANA@correo.com")).hasMessage("Este correo ya está registrado");
        assertThatThrownBy(() -> auth.registrar(new RegistroCmd("Ana", "b@c.co", "corta"))).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void loginBloqueaTrasCincoFallosYNoRevelaElCampo() {
        registrar("ana@correo.com");
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> auth.iniciarSesion("ana@correo.com", "mala-clave-xx"))
                    .hasMessage("Correo o contraseña incorrectos");
        }
        assertThatThrownBy(() -> auth.iniciarSesion("ana@correo.com", "mala-clave-xx"))
                .extracting(e -> ((ReglaNegocioException) e).getCodigo()).isEqualTo("CUENTA_BLOQUEADA");
        assertThatThrownBy(() -> auth.iniciarSesion("ana@correo.com", "clave-segura-1"))
                .extracting(e -> ((ReglaNegocioException) e).getCodigo()).isEqualTo("CUENTA_BLOQUEADA");
        assertThatThrownBy(() -> auth.iniciarSesion("nadie@correo.com", "x"))
                .hasMessage("Correo o contraseña incorrectos");
    }

    @Test
    void renovarRotaElTokenYCerrarSesionLoRevoca() {
        registrar("ana@correo.com");
        TokensDTO t1 = auth.iniciarSesion("ana@correo.com", "clave-segura-1");
        TokensDTO t2 = auth.renovarSesion(t1.refreshToken());
        auth.renovarSesion(t1.refreshToken());   // grace: one more use (double reload)
        assertThatThrownBy(() -> auth.renovarSesion(t1.refreshToken())).isInstanceOf(ReglaNegocioException.class);
        auth.cerrarSesion(t2.refreshToken());
        assertThatThrownBy(() -> auth.renovarSesion(t2.refreshToken())).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void recuperacionNoRevelaCorreosYElEnlaceEsDeUnSoloUso() {
        UUID id = registrar("ana@correo.com");
        auth.iniciarSesion("ana@correo.com", "clave-segura-1");
        auth.solicitarRecuperacion("nadie@correo.com");
        assertThat(notificador.enlaces).isEmpty();
        auth.solicitarRecuperacion("ana@correo.com");
        String token = notificador.enlaces.get(0).substring(notificador.enlaces.get(0).indexOf("token=") + 6);
        auth.restablecerContrasena(token, "nueva-clave-123");
        assertThat(emisor.sesiones).doesNotContainValue(id);
        assertThatThrownBy(() -> auth.restablecerContrasena(token, "otra-clave-123")).isInstanceOf(ReglaNegocioException.class);
        assertThat(auth.iniciarSesion("ana@correo.com", "nueva-clave-123")).isNotNull();
    }

    @Test
    void cuentaCambiaContrasenaYSeEliminaLogicamente() {
        UUID id = registrar("ana@correo.com");
        TokensDTO vieja = auth.iniciarSesion("ana@correo.com", "clave-segura-1");
        assertThatThrownBy(() -> cuenta.cambiarContrasena(id, "incorrecta-1", "nueva-clave-123"))
                .hasMessage("La contraseña actual no es correcta");
        TokensDTO nueva = cuenta.cambiarContrasena(id, "clave-segura-1", "nueva-clave-123");
        assertThat(emisor.sesiones).doesNotContainKey(vieja.refreshToken()).containsKey(nueva.refreshToken());
        UsuarioDTO u = cuenta.actualizarNombre(id, "Ana María");
        assertThat(u.nombre()).isEqualTo("Ana María");
        cuenta.eliminar(id);
        assertThat(emisor.sesiones).isEmpty();
        assertThatThrownBy(() -> auth.iniciarSesion("ana@correo.com", "nueva-clave-123"))
                .hasMessage("Correo o contraseña incorrectos");
        assertThatThrownBy(() -> cuenta.obtener(id)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void gastoConCategoriaDeIngresoODeOtroUsuarioFalla() {
        UUID ana = registrar("ana@correo.com");
        UUID beto = registrar("beto@correo.com");
        assertThatThrownBy(() -> gasto(ana, "10", salario.getId())).hasMessage("La categoría no corresponde al tipo de movimiento");
        UUID propiaDeBeto = cats.crear(beto, "Café", TipoMovimiento.GASTO, null).id();
        assertThatThrownBy(() -> gasto(ana, "10", propiaDeBeto)).hasMessage("La categoría no existe");
    }

    @Test
    void movimientoDeOtroUsuarioDa404() {
        UUID ana = registrar("ana@correo.com");
        UUID beto = registrar("beto@correo.com");
        UUID id = gasto(ana, "10", comida.getId()).id();
        assertThatThrownBy(() -> movs.eliminar(beto, id)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> movs.modificar(beto, id, new ModificarMovimientoCmd(BigDecimal.ONE, hoy, null,
                comida.getId(), null, null))).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void alertaDePresupuestoSeEnviaSoloLaPrimeraVez() {
        UUID ana = registrar("ana@correo.com");
        press.definir(ana, comida.getId(), YearMonth.from(hoy), Dinero.de(new BigDecimal("100")));
        gasto(ana, "90", comida.getId());
        assertThat(notificador.alertas).isEmpty();
        gasto(ana, "20", comida.getId());
        gasto(ana, "5", comida.getId());
        assertThat(notificador.alertas).hasSize(1);
        assertThat(press.listar(ana, YearMonth.from(hoy)).get(0).estado().name()).isEqualTo("EXCEDIDO");
    }

    @Test
    void sinCanalDeCorreoLaAlertaNoSeDaPorEnviada() {
        UUID ana = registrar("ana@correo.com");
        press.definir(ana, comida.getId(), YearMonth.from(hoy), Dinero.de(new BigDecimal("100")));
        notificador.disponible = false;
        gasto(ana, "120", comida.getId());
        assertThat(notificador.alertas).isEmpty();
        notificador.disponible = true;   // the channel comes back: the next expense sends the pending alert once
        gasto(ana, "1", comida.getId());
        gasto(ana, "1", comida.getId());
        assertThat(notificador.alertas).hasSize(1);
    }

    @Test
    void eliminarCategoriaReasignaMovimientosYBorraSusPresupuestos() {
        UUID ana = registrar("ana@correo.com");
        UUID cafe = cats.crear(ana, "Café", TipoMovimiento.GASTO, null).id();
        gasto(ana, "8", cafe);
        press.definir(ana, cafe, YearMonth.from(hoy), Dinero.de(new BigDecimal("50")));
        assertThatThrownBy(() -> cats.eliminar(ana, cafe, null)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> cats.crear(ana, "café", TipoMovimiento.GASTO, null)).hasMessage("Ya existe una categoría con ese nombre");
        cats.eliminar(ana, cafe, comida.getId());
        assertThat(movimientos.contarPorCategoria(ana, comida.getId())).isEqualTo(1);
        assertThat(presupuestos.datos).isEmpty();
        assertThatThrownBy(() -> cats.eliminar(ana, comida.getId(), null)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void resumenCalculaBalanceYPorcentajes() {
        UUID ana = registrar("ana@correo.com");
        UUID transporte = cats.crear(ana, "Transporte", TipoMovimiento.GASTO, null).id();
        movs.registrarIngreso(new RegistrarIngresoCmd(ana, new BigDecimal("1000"), hoy, null, salario.getId(), "Empresa"));
        gasto(ana, "300", comida.getId());
        gasto(ana, "100", transporte);
        ResumenFinanciero r = consulta.obtenerResumen(ana, hoy.withDayOfMonth(1), hoy);
        assertThat(r.totalIngresos()).isEqualByComparingTo("1000");
        assertThat(r.totalGastos()).isEqualByComparingTo("400");
        assertThat(r.balance()).isEqualByComparingTo("600");
        assertThat(r.porCategoria()).extracting(t -> t.porcentaje().toPlainString()).containsExactly("75.0", "25.0");
        // Trend: weeks of the month that add up to the totals; no previous data, so no comparison.
        assertThat(r.tendencia()).isNotEmpty().first().extracting(p -> p.etiqueta()).isEqualTo("Sem 1");
        assertThat(r.tendencia().stream().map(p -> p.gastos()).reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("400");
        assertThat(r.variacionGastos()).isNull();
    }

    @Test
    void tendenciaDeVariosMesesSeAgrupaPorMes() {
        var puntos = co.finanzas.aplicacion.servicios.ServicioConsultaFinanciera.tendencia(java.util.List.of(),
                java.time.LocalDate.of(2026, 1, 15), java.time.LocalDate.of(2026, 4, 10), 86);
        assertThat(puntos).extracting(p -> p.etiqueta()).containsExactly("ene 2026", "feb 2026", "mar 2026", "abr 2026");
    }

    @Test
    void historialValidaRangoYTamano() {
        UUID ana = registrar("ana@correo.com");
        for (int i = 0; i < 25; i++) {
            gasto(ana, "1", comida.getId());
        }
        FiltroMovimientos mes = new FiltroMovimientos(hoy.withDayOfMonth(1), hoy, null, null);
        assertThat(consulta.consultarHistorial(ana, mes, 1, 20).elementos()).hasSize(5);
        assertThatThrownBy(() -> consulta.consultarHistorial(ana, mes, 0, 101)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> consulta.consultarHistorial(ana, new FiltroMovimientos(hoy, hoy.minusDays(1), null, null), 0, 20))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> consulta.consultarHistorial(ana, new FiltroMovimientos(hoy.minusMonths(25), hoy, null, null), 0, 20))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> consulta.consultarHistorial(ana,
                new FiltroMovimientos(hoy, hoy, null, null, "x".repeat(FiltroMovimientos.MAX_TEXTO + 1)), 0, 20))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(new FiltroMovimientos(hoy, hoy, null, null, "   ").texto()).isNull();
    }
}
