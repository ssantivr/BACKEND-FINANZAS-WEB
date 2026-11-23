package co.finanzas.aplicacion.servicios;

import co.finanzas.aplicacion.dto.FiltroMovimientos;
import co.finanzas.aplicacion.dto.MovimientoDTO;
import co.finanzas.aplicacion.dto.Pagina;
import co.finanzas.aplicacion.dto.PresupuestoDTO;
import co.finanzas.aplicacion.dto.PuntoTendencia;
import co.finanzas.aplicacion.dto.TotalDiario;
import co.finanzas.aplicacion.dto.ResumenFinanciero;
import co.finanzas.aplicacion.dto.TotalPorCategoria;
import co.finanzas.aplicacion.puertos.entrada.IConsultaFinanciera;
import co.finanzas.aplicacion.puertos.salida.IRepositorioMovimientos;
import co.finanzas.aplicacion.puertos.salida.IRepositorioPresupuestos;
import co.finanzas.dominio.excepciones.RecursoNoEncontradoException;
import co.finanzas.dominio.excepciones.ReglaNegocioException;
import co.finanzas.dominio.tipos.TipoMovimiento;
import co.finanzas.dominio.valor.Dinero;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ServicioConsultaFinanciera implements IConsultaFinanciera {

    public static final int TAMANO_MAXIMO = 100;   // RF-21

    private final IRepositorioMovimientos repoMovimientos;
    private final IRepositorioPresupuestos repoPresupuestos;

    private final Clock reloj;

    public ServicioConsultaFinanciera(IRepositorioMovimientos repoMovimientos, IRepositorioPresupuestos repoPresupuestos,
                                      Clock reloj) {
        this.repoMovimientos = repoMovimientos;
        this.repoPresupuestos = repoPresupuestos;
        this.reloj = reloj;
    }

    @Override
    public Pagina<MovimientoDTO> consultarHistorial(UUID idUsuario, FiltroMovimientos filtro, int pagina, int tamano) {
        filtro.validar();
        if (pagina < 0 || tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new ReglaNegocioException("PAGINA_INVALIDA", "La página debe ser >= 0 y el tamaño entre 1 y 100");
        }
        return repoMovimientos.buscar(idUsuario, filtro, pagina, tamano).map(MovimientoDTO::de);
    }

    @Override
    public MovimientoDTO obtenerMovimiento(UUID idUsuario, UUID idMovimiento) {
        return repoMovimientos.buscarDeUsuario(idUsuario, idMovimiento).map(MovimientoDTO::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("Movimiento", idMovimiento));
    }

    @Override
    public ResumenFinanciero obtenerResumen(UUID idUsuario, LocalDate desde, LocalDate hasta) {
        new FiltroMovimientos(desde, hasta, null, null).validar();
        List<TotalPorCategoria> totales = repoMovimientos.sumarPorCategoria(idUsuario, desde, hasta);
        Dinero ingresos = sumar(totales, TipoMovimiento.INGRESO);
        Dinero gastos = sumar(totales, TipoMovimiento.GASTO);
        List<TotalPorCategoria> porCategoria = totales.stream()
                .filter(t -> t.categoria().tipo() == TipoMovimiento.GASTO)
                .map(t -> new TotalPorCategoria(t.categoria(), t.total(), Dinero.de(t.total()).porcentajeDe(gastos)))
                .sorted(Comparator.comparing(TotalPorCategoria::total).reversed())
                .toList();
        List<PresupuestoDTO> presupuestos = repoPresupuestos.buscarPorPeriodo(idUsuario, YearMonth.from(hasta)).stream()
                .map(p -> PresupuestoDTO.de(p, repoMovimientos.sumarGastos(idUsuario, p.getCategoria().getId(), p.getPeriodo())))
                .toList();
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        List<TotalPorCategoria> previos = repoMovimientos.sumarPorCategoria(idUsuario, desde.minusDays(dias), desde.minusDays(1));
        // Daily average over the days already lived: "this month" on the 17th divides by 17, not 30.
        LocalDate hoy = LocalDate.now(reloj);
        long transcurridos = hasta.isAfter(hoy) && !desde.isAfter(hoy) ? ChronoUnit.DAYS.between(desde, hoy) + 1 : dias;
        BigDecimal promedioDiario = gastos.getCantidad().divide(BigDecimal.valueOf(transcurridos), 2, RoundingMode.HALF_UP);
        return new ResumenFinanciero(desde, hasta, ingresos.getCantidad(), gastos.getCantidad(),
                ingresos.restar(gastos).getCantidad(), porCategoria, presupuestos,
                variacion(ingresos, sumar(previos, TipoMovimiento.INGRESO)), variacion(gastos, sumar(previos, TipoMovimiento.GASTO)),
                promedioDiario, tendencia(repoMovimientos.sumarPorDia(idUsuario, desde, hasta), desde, hasta, dias));
    }

    /** Weeks for ranges up to 62 days ("Sem 1", "Sem 2"...), calendar months otherwise ("ene 2026"). */
    public static List<PuntoTendencia> tendencia(List<TotalDiario> diarios, LocalDate desde, LocalDate hasta, long dias) {
        List<PuntoTendencia> puntos = new ArrayList<>();
        LocalDate inicio = desde;
        int n = 1;
        while (!inicio.isAfter(hasta)) {
            LocalDate fin = dias <= 62 ? inicio.plusDays(6) : YearMonth.from(inicio).atEndOfMonth();
            if (fin.isAfter(hasta)) {
                fin = hasta;
            }
            String etiqueta = dias <= 62 ? "Sem " + n
                    : inicio.getMonth().getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es-CO")) + " " + inicio.getYear();
            LocalDate a = inicio;
            LocalDate b = fin;
            puntos.add(new PuntoTendencia(etiqueta, a, b, sumarDias(diarios, a, b, TipoMovimiento.INGRESO),
                    sumarDias(diarios, a, b, TipoMovimiento.GASTO)));
            inicio = fin.plusDays(1);
            n++;
        }
        return puntos;
    }

    private static BigDecimal sumarDias(List<TotalDiario> diarios, LocalDate a, LocalDate b, TipoMovimiento tipo) {
        return diarios.stream()
                .filter(d -> d.tipo() == tipo && !d.fecha().isBefore(a) && !d.fecha().isAfter(b))
                .map(TotalDiario::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal variacion(Dinero actual, Dinero anterior) {
        if (anterior.getCantidad().signum() == 0) {
            return null;
        }
        return actual.getCantidad().subtract(anterior.getCantidad()).multiply(BigDecimal.valueOf(100))
                .divide(anterior.getCantidad(), 1, RoundingMode.HALF_UP);
    }

    private static Dinero sumar(List<TotalPorCategoria> totales, TipoMovimiento tipo) {
        return totales.stream()
                .filter(t -> t.categoria().tipo() == tipo)
                .map(t -> Dinero.de(t.total()))
                .reduce(Dinero.cero(Dinero.COP), Dinero::sumar);
    }
}
