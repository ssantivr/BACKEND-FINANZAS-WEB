package co.finanzas.infraestructura.persistencia;

import co.finanzas.aplicacion.dto.CategoriaDTO;
import co.finanzas.aplicacion.dto.FiltroMovimientos;
import co.finanzas.aplicacion.dto.Pagina;
import co.finanzas.aplicacion.dto.TotalDiario;
import co.finanzas.aplicacion.dto.TotalPorCategoria;
import co.finanzas.aplicacion.puertos.salida.IRepositorioMovimientos;
import co.finanzas.dominio.modelo.Categoria;
import co.finanzas.dominio.modelo.Gasto;
import co.finanzas.dominio.modelo.Ingreso;
import co.finanzas.dominio.modelo.Movimiento;
import co.finanzas.dominio.tipos.MetodoPago;
import co.finanzas.dominio.tipos.TipoMovimiento;
import co.finanzas.dominio.valor.Dinero;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class RepositorioMovimientosAdaptador implements IRepositorioMovimientos {

    /** Stored movements were valid when saved; rebuilding them must not fail because of today's date. */
    private static final LocalDate SIN_LIMITE = LocalDate.of(9999, 1, 1);

    private final MovimientoRepositorioMongo mongo;
    private final MongoTemplate plantilla;
    private final RepositorioCategoriasAdaptador categorias;

    public RepositorioMovimientosAdaptador(MovimientoRepositorioMongo mongo, MongoTemplate plantilla,
                                           RepositorioCategoriasAdaptador categorias) {
        this.mongo = mongo;
        this.plantilla = plantilla;
        this.categorias = categorias;
    }

    @Override
    public void guardar(Movimiento m) {
        MovimientoDocumento d = new MovimientoDocumento();
        d.id = m.getId().toString();
        d.idUsuario = m.getIdUsuario().toString();
        d.tipo = m.getTipo().name();
        d.monto = m.getMonto().getCantidad();
        d.moneda = m.getMonto().getMoneda().getCurrencyCode();
        d.fecha = m.getFecha().toString();
        d.descripcion = m.getDescripcion();
        d.idCategoria = m.getCategoria().getId().toString();
        d.metodoPago = m instanceof Gasto g ? g.getMetodoPago().name() : null;
        d.fuente = m instanceof Ingreso i ? i.getFuente() : null;
        d.fechaRegistro = m.getFechaRegistro();
        mongo.save(d);
    }

    @Override
    public void eliminar(Movimiento m) {
        mongo.deleteById(m.getId().toString());
    }

    @Override
    public Optional<Movimiento> buscarDeUsuario(UUID idUsuario, UUID id) {
        return mongo.findByIdAndIdUsuario(id.toString(), idUsuario.toString())
                .map(d -> aDominio(List.of(d)).get(0));
    }

    @Override
    public Pagina<Movimiento> buscar(UUID idUsuario, FiltroMovimientos filtro, int pagina, int tamano) {
        Criteria c = Criteria.where("idUsuario").is(idUsuario.toString())
                .and("fecha").gte(filtro.desde().toString()).lte(filtro.hasta().toString());
        if (filtro.tipo() != null) {
            c = c.and("tipo").is(filtro.tipo().name());
        }
        if (filtro.idCategoria() != null) {
            c = c.and("idCategoria").is(filtro.idCategoria().toString());
        }
        if (filtro.texto() != null) {
            c = c.andOperator(coincideTexto(idUsuario, filtro.texto()));
        }
        Query q = Query.query(c);
        long total = plantilla.count(q, MovimientoDocumento.class);
        q.with(Sort.by(Sort.Direction.DESC, "fecha", "fechaRegistro")).skip((long) pagina * tamano).limit(tamano);
        List<Movimiento> elementos = aDominio(plantilla.find(q, MovimientoDocumento.class));
        return new Pagina<>(elementos, pagina, tamano, total);
    }

    /** Literal, case-insensitive match (the user's text is quoted, never used as a regex). */
    private Criteria coincideTexto(UUID idUsuario, String texto) {
        Pattern patron = Pattern.compile(Pattern.quote(texto), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        List<String> idsCategoria = plantilla.find(Query.query(new Criteria().andOperator(
                        new Criteria().orOperator(Criteria.where("idPropietario").is(null),
                                Criteria.where("idPropietario").is(idUsuario.toString())),
                        Criteria.where("nombre").regex(patron))), CategoriaDocumento.class)
                .stream().map(d -> d.id).toList();
        return new Criteria().orOperator(Criteria.where("descripcion").regex(patron),
                Criteria.where("fuente").regex(patron), Criteria.where("idCategoria").in(idsCategoria));
    }

    @Override
    public List<TotalPorCategoria> sumarPorCategoria(UUID idUsuario, LocalDate desde, LocalDate hasta) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("idUsuario").is(idUsuario.toString())
                        .and("fecha").gte(desde.toString()).lte(hasta.toString())),
                Aggregation.group("idCategoria").sum("monto").as("total"));
        List<Document> filas = plantilla.aggregate(agg, MovimientoDocumento.class, Document.class).getMappedResults();
        Map<String, Categoria> cats = categorias.porIds(filas.stream().map(f -> f.getString("_id")).toList());
        return filas.stream()
                .filter(f -> cats.containsKey(f.getString("_id")))
                .map(f -> new TotalPorCategoria(CategoriaDTO.de(cats.get(f.getString("_id"))),
                        Dinero.de(decimal(f.get("total"))).getCantidad(), null))
                .toList();
    }

    @Override
    public List<TotalDiario> sumarPorDia(UUID idUsuario, LocalDate desde, LocalDate hasta) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("idUsuario").is(idUsuario.toString())
                        .and("fecha").gte(desde.toString()).lte(hasta.toString())),
                Aggregation.group("fecha", "tipo").sum("monto").as("total"));
        return plantilla.aggregate(agg, MovimientoDocumento.class, Document.class).getMappedResults().stream()
                .map(f -> {
                    Document id = f.get("_id", Document.class);
                    return new TotalDiario(LocalDate.parse(id.getString("fecha")), TipoMovimiento.valueOf(id.getString("tipo")),
                            decimal(f.get("total")));
                })
                .toList();
    }

    @Override
    public Dinero sumarGastos(UUID idUsuario, UUID idCategoria, YearMonth periodo) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("idUsuario").is(idUsuario.toString())
                        .and("idCategoria").is(idCategoria.toString())
                        .and("tipo").is(TipoMovimiento.GASTO.name())
                        .and("fecha").gte(periodo.atDay(1).toString()).lte(periodo.atEndOfMonth().toString())),
                Aggregation.group().sum("monto").as("total"));
        Document fila = plantilla.aggregate(agg, MovimientoDocumento.class, Document.class).getUniqueMappedResult();
        return Dinero.de(fila == null ? BigDecimal.ZERO : decimal(fila.get("total")));
    }

    @Override
    public long contarPorCategoria(UUID idUsuario, UUID idCategoria) {
        return mongo.countByIdUsuarioAndIdCategoria(idUsuario.toString(), idCategoria.toString());
    }

    @Override
    public int reasignarCategoria(UUID idUsuario, UUID origen, UUID destino) {
        Query q = Query.query(Criteria.where("idUsuario").is(idUsuario.toString()).and("idCategoria").is(origen.toString()));
        return (int) plantilla.updateMulti(q, Update.update("idCategoria", destino.toString()), MovimientoDocumento.class)
                .getModifiedCount();
    }

    private static BigDecimal decimal(Object valor) {
        if (valor instanceof Decimal128 d) {
            return d.bigDecimalValue();
        }
        return valor == null ? BigDecimal.ZERO : new BigDecimal(valor.toString());
    }

    private List<Movimiento> aDominio(List<MovimientoDocumento> docs) {
        Set<String> ids = docs.stream().map(d -> d.idCategoria).collect(Collectors.toSet());
        Map<String, Categoria> cats = categorias.porIds(ids);
        return docs.stream().map(d -> aDominio(d, Objects.requireNonNull(cats.get(d.idCategoria),
                "Movimiento con categoría inexistente: " + d.id))).toList();
    }

    private static Movimiento aDominio(MovimientoDocumento d, Categoria categoria) {
        Dinero monto = new Dinero(d.monto, Currency.getInstance(d.moneda));
        UUID id = UUID.fromString(d.id);
        UUID idUsuario = UUID.fromString(d.idUsuario);
        LocalDate fecha = LocalDate.parse(d.fecha);
        if (TipoMovimiento.valueOf(d.tipo) == TipoMovimiento.GASTO) {
            return new Gasto(id, idUsuario, monto, fecha, d.descripcion, categoria, MetodoPago.valueOf(d.metodoPago),
                    d.fechaRegistro, SIN_LIMITE);
        }
        return new Ingreso(id, idUsuario, monto, fecha, d.descripcion, categoria, d.fuente, d.fechaRegistro, SIN_LIMITE);
    }
}
