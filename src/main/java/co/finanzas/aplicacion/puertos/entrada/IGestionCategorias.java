package co.finanzas.aplicacion.puertos.entrada;

import co.finanzas.aplicacion.dto.CategoriaDTO;
import co.finanzas.dominio.tipos.TipoMovimiento;

import java.util.List;
import java.util.UUID;

public interface IGestionCategorias {
    List<CategoriaDTO> listarDisponibles(UUID idUsuario, TipoMovimiento tipo);
    CategoriaDTO crear(UUID idUsuario, String nombre, TipoMovimiento tipo, String color);
    CategoriaDTO renombrar(UUID idUsuario, UUID idCategoria, String nombre);
    void eliminar(UUID idUsuario, UUID idCategoria, UUID idCategoriaDestino);
}
