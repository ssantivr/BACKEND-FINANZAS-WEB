package co.finanzas.aplicacion.servicios;

import co.finanzas.aplicacion.dto.PresupuestoDTO;
import co.finanzas.aplicacion.puertos.entrada.IEvaluadorPresupuesto;
import co.finanzas.aplicacion.puertos.entrada.IGestionPresupuestos;
import co.finanzas.aplicacion.puertos.salida.INotificador;
import co.finanzas.aplicacion.puertos.salida.IRepositorioCategorias;
import co.finanzas.aplicacion.puertos.salida.IRepositorioMovimientos;
import co.finanzas.aplicacion.puertos.salida.IRepositorioPresupuestos;
import co.finanzas.aplicacion.puertos.salida.IRepositorioUsuarios;
import co.finanzas.dominio.excepciones.RecursoNoEncontradoException;
import co.finanzas.dominio.excepciones.ReglaNegocioException;
import co.finanzas.dominio.modelo.Categoria;
import co.finanzas.dominio.modelo.Gasto;
import co.finanzas.dominio.modelo.Presupuesto;
import co.finanzas.dominio.valor.Dinero;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public class ServicioPresupuestos implements IGestionPresupuestos, IEvaluadorPresupuesto {

    private final IRepositorioPresupuestos repoPresupuestos;
    private final IRepositorioMovimientos repoMovimientos;
    private final IRepositorioCategorias repoCategorias;
    private final IRepositorioUsuarios repoUsuarios;
    private final INotificador notificador;

    public ServicioPresupuestos(IRepositorioPresupuestos repoPresupuestos, IRepositorioMovimientos repoMovimientos,
                                IRepositorioCategorias repoCategorias, IRepositorioUsuarios repoUsuarios,
                                INotificador notificador) {
        this.repoPresupuestos = repoPresupuestos;
        this.repoMovimientos = repoMovimientos;
        this.repoCategorias = repoCategorias;
        this.repoUsuarios = repoUsuarios;
        this.notificador = notificador;
    }

    @Override
    public PresupuestoDTO definir(UUID idUsuario, UUID idCategoria, YearMonth periodo, Dinero limite) {
        if (periodo == null) {
            throw new ReglaNegocioException("PERIODO_OBLIGATORIO", "El mes del presupuesto es obligatorio");
        }
        Categoria categoria = repoCategorias.buscarDisponible(idCategoria, idUsuario)
                .orElseThrow(() -> new ReglaNegocioException("CATEGORIA_INVALIDA", "La categoría no existe"));
        Presupuesto p = repoPresupuestos.buscar(idUsuario, idCategoria, periodo)
                .map(existente -> {
                    existente.cambiarLimite(limite);
                    return existente;
                })
                .orElseGet(() -> new Presupuesto(UUID.randomUUID(), idUsuario, categoria, periodo, limite, false));
        repoPresupuestos.guardar(p);
        return PresupuestoDTO.de(p, consumo(p));
    }

    @Override
    public List<PresupuestoDTO> listar(UUID idUsuario, YearMonth periodo) {
        return repoPresupuestos.buscarPorPeriodo(idUsuario, periodo).stream()
                .map(p -> PresupuestoDTO.de(p, consumo(p)))
                .toList();
    }

    @Override
    public void eliminar(UUID idUsuario, UUID idPresupuesto) {
        Presupuesto p = repoPresupuestos.buscarPorId(idUsuario, idPresupuesto)
                .orElseThrow(() -> new RecursoNoEncontradoException("Presupuesto", idPresupuesto));
        repoPresupuestos.eliminar(p);
    }

    /** RN-12: e-mail only the first time the budget is exceeded in the month. */
    @Override
    public void evaluar(Gasto gasto) {
        YearMonth periodo = YearMonth.from(gasto.getFecha());
        repoPresupuestos.buscar(gasto.getIdUsuario(), gasto.getCategoria().getId(), periodo).ifPresent(p -> {
            Dinero consumo = consumo(p);
            if (p.debeNotificar(consumo) && notificador.puedeEnviar()) {
                p.marcarNotificado();
                repoPresupuestos.guardar(p);
                repoUsuarios.buscarPorId(gasto.getIdUsuario()).ifPresent(u ->
                        notificador.enviarAlertaPresupuesto(u.getCorreo(), PresupuestoDTO.de(p, consumo)));
            } else if (p.reiniciarAvisoSiVolvioAlLimite(consumo)) {
                repoPresupuestos.guardar(p);
            }
        });
    }

    private Dinero consumo(Presupuesto p) {
        return repoMovimientos.sumarGastos(p.getIdUsuario(), p.getCategoria().getId(), p.getPeriodo());
    }
}
