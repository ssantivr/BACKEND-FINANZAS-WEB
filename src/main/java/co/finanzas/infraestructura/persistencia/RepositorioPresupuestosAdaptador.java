package co.finanzas.infraestructura.persistencia;

import co.finanzas.aplicacion.puertos.salida.IRepositorioPresupuestos;
import co.finanzas.dominio.modelo.Categoria;
import co.finanzas.dominio.modelo.Presupuesto;
import co.finanzas.dominio.valor.Dinero;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class RepositorioPresupuestosAdaptador implements IRepositorioPresupuestos {

    private final PresupuestoRepositorioMongo mongo;
    private final RepositorioCategoriasAdaptador categorias;

    public RepositorioPresupuestosAdaptador(PresupuestoRepositorioMongo mongo, RepositorioCategoriasAdaptador categorias) {
        this.mongo = mongo;
        this.categorias = categorias;
    }

    @Override
    public void guardar(Presupuesto p) {
        PresupuestoDocumento d = new PresupuestoDocumento();
        d.id = p.getId().toString();
        d.idUsuario = p.getIdUsuario().toString();
        d.idCategoria = p.getCategoria().getId().toString();
        d.periodo = p.getPeriodo().toString();
        d.limite = p.getLimite().getCantidad();
        d.moneda = p.getLimite().getMoneda().getCurrencyCode();
        d.notificado = p.isNotificado();
        mongo.save(d);
    }

    @Override
    public void eliminar(Presupuesto p) {
        mongo.deleteById(p.getId().toString());
    }

    @Override
    public Optional<Presupuesto> buscar(UUID idUsuario, UUID idCategoria, YearMonth periodo) {
        return mongo.findByIdUsuarioAndIdCategoriaAndPeriodo(idUsuario.toString(), idCategoria.toString(), periodo.toString())
                .flatMap(this::aDominio);
    }

    @Override
    public Optional<Presupuesto> buscarPorId(UUID idUsuario, UUID id) {
        return mongo.findByIdAndIdUsuario(id.toString(), idUsuario.toString()).flatMap(this::aDominio);
    }

    @Override
    public List<Presupuesto> buscarPorPeriodo(UUID idUsuario, YearMonth periodo) {
        List<PresupuestoDocumento> docs = mongo.findByIdUsuarioAndPeriodo(idUsuario.toString(), periodo.toString());
        Map<String, Categoria> cats = categorias.porIds(docs.stream().map(d -> d.idCategoria).toList());
        return docs.stream().filter(d -> cats.containsKey(d.idCategoria))
                .map(d -> aDominio(d, cats.get(d.idCategoria)))
                .sorted((a, b) -> a.getCategoria().getNombre().compareToIgnoreCase(b.getCategoria().getNombre()))
                .toList();
    }

    @Override
    public void eliminarPorCategoria(UUID idUsuario, UUID idCategoria) {
        mongo.deleteByIdUsuarioAndIdCategoria(idUsuario.toString(), idCategoria.toString());
    }

    private Optional<Presupuesto> aDominio(PresupuestoDocumento d) {
        return Optional.ofNullable(categorias.porIds(List.of(d.idCategoria)).get(d.idCategoria)).map(c -> aDominio(d, c));
    }

    private static Presupuesto aDominio(PresupuestoDocumento d, Categoria c) {
        return new Presupuesto(UUID.fromString(d.id), UUID.fromString(d.idUsuario), c, YearMonth.parse(d.periodo),
                new Dinero(d.limite, Currency.getInstance(d.moneda)), d.notificado);
    }
}
