package co.finanzas.infraestructura.persistencia;

import co.finanzas.aplicacion.puertos.salida.IRepositorioCategorias;
import co.finanzas.dominio.excepciones.ReglaNegocioException;
import co.finanzas.dominio.modelo.Categoria;
import co.finanzas.dominio.tipos.TipoMovimiento;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RepositorioCategoriasAdaptador implements IRepositorioCategorias {

    private final CategoriaRepositorioMongo mongo;
    private final MongoTemplate plantilla;

    public RepositorioCategoriasAdaptador(CategoriaRepositorioMongo mongo, MongoTemplate plantilla) {
        this.mongo = mongo;
        this.plantilla = plantilla;
    }

    @Override
    public void guardar(Categoria c) {
        try {
            mongo.save(aDocumento(c));
        } catch (DuplicateKeyException e) {
            throw new ReglaNegocioException("CATEGORIA_DUPLICADA", "Ya existe una categoría con ese nombre");
        }
    }

    @Override
    public void eliminar(Categoria c) {
        mongo.deleteById(c.getId().toString());
    }

    @Override
    public Optional<Categoria> buscarDisponible(UUID id, UUID idUsuario) {
        return mongo.findById(id.toString()).map(RepositorioCategoriasAdaptador::aDominio)
                .filter(c -> c.estaDisponiblePara(idUsuario));
    }

    @Override
    public List<Categoria> listarDisponibles(UUID idUsuario, TipoMovimiento tipo) {
        Criteria criterio = new Criteria().orOperator(
                Criteria.where("idPropietario").is(null),
                Criteria.where("idPropietario").is(idUsuario.toString()));
        if (tipo != null) {
            criterio = new Criteria().andOperator(criterio, Criteria.where("tipo").is(tipo.name()));
        }
        Query q = Query.query(criterio).with(Sort.by("tipo", "nombreClave"));
        return plantilla.find(q, CategoriaDocumento.class).stream().map(RepositorioCategoriasAdaptador::aDominio).toList();
    }

    /** Used by the other adapters to rebuild aggregates without one query per document. */
    Map<String, Categoria> porIds(Collection<String> ids) {
        return mongo.findAllById(ids).stream().map(RepositorioCategoriasAdaptador::aDominio)
                .collect(Collectors.toMap(c -> c.getId().toString(), Function.identity()));
    }

    static CategoriaDocumento aDocumento(Categoria c) {
        CategoriaDocumento d = new CategoriaDocumento();
        d.id = c.getId().toString();
        d.nombre = c.getNombre();
        d.nombreClave = c.getNombre().toLowerCase(Locale.ROOT);
        d.tipo = c.getTipo().name();
        d.icono = c.getIcono();
        d.color = c.getColor();
        d.idPropietario = c.getIdPropietario() == null ? null : c.getIdPropietario().toString();
        return d;
    }

    static Categoria aDominio(CategoriaDocumento d) {
        return new Categoria(UUID.fromString(d.id), d.nombre, TipoMovimiento.valueOf(d.tipo), d.icono, d.color,
                d.idPropietario == null ? null : UUID.fromString(d.idPropietario));
    }
}
