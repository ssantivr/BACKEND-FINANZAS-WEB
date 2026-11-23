package co.finanzas.aplicacion.puertos.salida;

import co.finanzas.dominio.modelo.Presupuesto;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IRepositorioPresupuestos {
    void guardar(Presupuesto p);
    void eliminar(Presupuesto p);
    Optional<Presupuesto> buscar(UUID idUsuario, UUID idCategoria, YearMonth periodo);
    Optional<Presupuesto> buscarPorId(UUID idUsuario, UUID id);
    List<Presupuesto> buscarPorPeriodo(UUID idUsuario, YearMonth periodo);
    void eliminarPorCategoria(UUID idUsuario, UUID idCategoria);
}
