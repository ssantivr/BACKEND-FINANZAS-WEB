package co.finanzas.aplicacion.puertos.salida;

import co.finanzas.dominio.modelo.Categoria;
import co.finanzas.dominio.tipos.TipoMovimiento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IRepositorioCategorias {
    /** Throws a ReglaNegocioException CATEGORIA_DUPLICADA when the name already exists for that user and type. */
    void guardar(Categoria c);
    void eliminar(Categoria c);
    Optional<Categoria> buscarDisponible(UUID id, UUID idUsuario);
    List<Categoria> listarDisponibles(UUID idUsuario, TipoMovimiento tipo);
}
