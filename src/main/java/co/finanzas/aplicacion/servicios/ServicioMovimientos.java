package co.finanzas.aplicacion.servicios;

import co.finanzas.aplicacion.dto.ModificarMovimientoCmd;
import co.finanzas.aplicacion.dto.MovimientoDTO;
import co.finanzas.aplicacion.dto.RegistrarGastoCmd;
import co.finanzas.aplicacion.dto.RegistrarIngresoCmd;
import co.finanzas.aplicacion.puertos.entrada.IEvaluadorPresupuesto;
import co.finanzas.aplicacion.puertos.entrada.IGestionMovimientos;
import co.finanzas.aplicacion.puertos.salida.IRepositorioCategorias;
import co.finanzas.aplicacion.puertos.salida.IRepositorioMovimientos;
import co.finanzas.dominio.excepciones.RecursoNoEncontradoException;
import co.finanzas.dominio.excepciones.ReglaNegocioException;
import co.finanzas.dominio.modelo.Categoria;
import co.finanzas.dominio.modelo.Gasto;
import co.finanzas.dominio.modelo.Ingreso;
import co.finanzas.dominio.modelo.Movimiento;
import co.finanzas.dominio.valor.Dinero;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class ServicioMovimientos implements IGestionMovimientos {

    private final IRepositorioMovimientos repoMovimientos;
    private final IRepositorioCategorias repoCategorias;
    private final IEvaluadorPresupuesto evaluador;
    private final Clock reloj;

    public ServicioMovimientos(IRepositorioMovimientos repoMovimientos, IRepositorioCategorias repoCategorias,
                               IEvaluadorPresupuesto evaluador, Clock reloj) {
        this.repoMovimientos = repoMovimientos;
        this.repoCategorias = repoCategorias;
        this.evaluador = evaluador;
        this.reloj = reloj;
    }

    @Override
    public MovimientoDTO registrarGasto(RegistrarGastoCmd cmd) {
        Gasto g = new Gasto(UUID.randomUUID(), cmd.idUsuario(), dinero(cmd.monto()), cmd.fecha(), cmd.descripcion(),
                categoria(cmd.idCategoria(), cmd.idUsuario()), cmd.metodoPago(), LocalDateTime.now(reloj), hoy());
        repoMovimientos.guardar(g);
        evaluador.evaluar(g);
        return MovimientoDTO.de(g);
    }

    @Override
    public MovimientoDTO registrarIngreso(RegistrarIngresoCmd cmd) {
        Ingreso i = new Ingreso(UUID.randomUUID(), cmd.idUsuario(), dinero(cmd.monto()), cmd.fecha(), cmd.descripcion(),
                categoria(cmd.idCategoria(), cmd.idUsuario()), cmd.fuente(), LocalDateTime.now(reloj), hoy());
        repoMovimientos.guardar(i);
        return MovimientoDTO.de(i);
    }

    @Override
    public MovimientoDTO modificar(UUID idUsuario, UUID idMovimiento, ModificarMovimientoCmd cmd) {
        Movimiento m = repoMovimientos.buscarDeUsuario(idUsuario, idMovimiento)
                .orElseThrow(() -> new RecursoNoEncontradoException("Movimiento", idMovimiento));
        m.modificar(dinero(cmd.monto()), cmd.fecha(), cmd.descripcion(), categoria(cmd.idCategoria(), idUsuario), hoy());
        if (m instanceof Gasto g && cmd.metodoPago() != null) {
            g.cambiarMetodoPago(cmd.metodoPago());
        }
        if (m instanceof Ingreso i) {
            i.cambiarFuente(cmd.fuente());
        }
        repoMovimientos.guardar(m);
        if (m instanceof Gasto g) {
            evaluador.evaluar(g);
        }
        return MovimientoDTO.de(m);
    }

    @Override
    public void eliminar(UUID idUsuario, UUID idMovimiento) {
        Movimiento m = repoMovimientos.buscarDeUsuario(idUsuario, idMovimiento)
                .orElseThrow(() -> new RecursoNoEncontradoException("Movimiento", idMovimiento));
        repoMovimientos.eliminar(m);
        if (m instanceof Gasto g) {
            evaluador.evaluar(g);   // spending went down: the budget alert can be re-armed
        }
    }

    /** RN-04: another user's category answers like a missing one, without revealing it exists. */
    private Categoria categoria(UUID idCategoria, UUID idUsuario) {
        if (idCategoria == null) {
            throw new ReglaNegocioException("CATEGORIA_INVALIDA", "La categoría es obligatoria");
        }
        return repoCategorias.buscarDisponible(idCategoria, idUsuario)
                .orElseThrow(() -> new ReglaNegocioException("CATEGORIA_INVALIDA", "La categoría no existe"));
    }

    private static Dinero dinero(BigDecimal monto) {
        if (monto == null) {
            throw new ReglaNegocioException("MONTO_INVALIDO", "El monto es obligatorio");
        }
        return Dinero.de(monto);
    }

    private LocalDate hoy() {
        return LocalDate.now(reloj);
    }
}
