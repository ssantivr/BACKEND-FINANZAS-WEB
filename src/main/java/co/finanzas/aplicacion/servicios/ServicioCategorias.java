package co.finanzas.aplicacion.servicios;

import co.finanzas.aplicacion.dto.CategoriaDTO;
import co.finanzas.aplicacion.puertos.entrada.IGestionCategorias;
import co.finanzas.aplicacion.puertos.salida.IRepositorioCategorias;
import co.finanzas.aplicacion.puertos.salida.IRepositorioMovimientos;
import co.finanzas.aplicacion.puertos.salida.IRepositorioPresupuestos;
import co.finanzas.dominio.excepciones.RecursoNoEncontradoException;
import co.finanzas.dominio.excepciones.ReglaNegocioException;
import co.finanzas.dominio.modelo.Categoria;
import co.finanzas.dominio.tipos.TipoMovimiento;

import java.util.List;
import java.util.UUID;

public class ServicioCategorias implements IGestionCategorias {

    private final IRepositorioCategorias repoCategorias;
    private final IRepositorioMovimientos repoMovimientos;
    private final IRepositorioPresupuestos repoPresupuestos;

    public ServicioCategorias(IRepositorioCategorias repoCategorias, IRepositorioMovimientos repoMovimientos,
                              IRepositorioPresupuestos repoPresupuestos) {
        this.repoCategorias = repoCategorias;
        this.repoMovimientos = repoMovimientos;
        this.repoPresupuestos = repoPresupuestos;
    }

    @Override
    public List<CategoriaDTO> listarDisponibles(UUID idUsuario, TipoMovimiento tipo) {
        return repoCategorias.listarDisponibles(idUsuario, tipo).stream().map(CategoriaDTO::de).toList();
    }

    @Override
    public CategoriaDTO crear(UUID idUsuario, String nombre, TipoMovimiento tipo, String color) {
        if (tipo == null) {
            throw new ReglaNegocioException("TIPO_OBLIGATORIO", "El tipo de la categoría es obligatorio");
        }
        Categoria c = Categoria.propia(nombre, tipo, color, idUsuario);
        repoCategorias.guardar(c);
        return CategoriaDTO.de(c);
    }

    @Override
    public CategoriaDTO renombrar(UUID idUsuario, UUID idCategoria, String nombre) {
        Categoria c = disponible(idUsuario, idCategoria);
        c.renombrar(nombre);
        repoCategorias.guardar(c);
        return CategoriaDTO.de(c);
    }

    @Override
    public void eliminar(UUID idUsuario, UUID idCategoria, UUID idCategoriaDestino) {
        Categoria origen = disponible(idUsuario, idCategoria);
        if (origen.esPredeterminada()) {
            throw new ReglaNegocioException("CATEGORIA_PREDETERMINADA", "Las categorías predeterminadas no se pueden eliminar");
        }
        if (repoMovimientos.contarPorCategoria(idUsuario, idCategoria) > 0) {
            if (idCategoriaDestino == null || idCategoriaDestino.equals(idCategoria)) {
                throw new ReglaNegocioException("DESTINO_OBLIGATORIO",
                        "La categoría tiene movimientos: elige una categoría destino del mismo tipo");
            }
            Categoria destino = repoCategorias.buscarDisponible(idCategoriaDestino, idUsuario)
                    .orElseThrow(() -> new ReglaNegocioException("CATEGORIA_INVALIDA", "La categoría destino no existe"));
            if (destino.getTipo() != origen.getTipo()) {
                throw new ReglaNegocioException("CATEGORIA_TIPO", "La categoría destino debe ser del mismo tipo");
            }
            repoMovimientos.reasignarCategoria(idUsuario, idCategoria, idCategoriaDestino);
        }
        repoPresupuestos.eliminarPorCategoria(idUsuario, idCategoria);   // no orphan budgets (RNF-05)
        repoCategorias.eliminar(origen);
    }

    private Categoria disponible(UUID idUsuario, UUID idCategoria) {
        return repoCategorias.buscarDisponible(idCategoria, idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría", idCategoria));
    }
}
