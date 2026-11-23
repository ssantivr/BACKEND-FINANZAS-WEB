package co.finanzas.infraestructura.configuracion;

import co.finanzas.dominio.modelo.Categoria;
import co.finanzas.dominio.tipos.TipoMovimiento;
import co.finanzas.infraestructura.persistencia.CategoriaRepositorioMongo;
import co.finanzas.aplicacion.puertos.salida.IRepositorioCategorias;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * RF-14: loads the predefined categories once (same ids as base-de-datos/03_categorias_predeterminadas.js),
 * so a new database works without running the scripts by hand.
 */
@Component
public class InicializadorCategorias implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(InicializadorCategorias.class);

    record Fila(String id, String nombre, TipoMovimiento tipo, String icono, String color) {
    }

    static final List<Fila> PREDETERMINADAS = List.of(
            new Fila("1c8868ac-9b89-4c04-a680-661c131c1165", "Alimentación", TipoMovimiento.GASTO, "cubiertos", "#14b8a6"),
            new Fila("1792c240-47cf-45c2-843f-09ca29df786b", "Transporte", TipoMovimiento.GASTO, "bus", "#f59e0b"),
            new Fila("f9097551-ede3-492e-bfcd-98cdbbd1b176", "Vivienda", TipoMovimiento.GASTO, "casa", "#0f766e"),
            new Fila("1a529716-8f75-4d3d-9903-a13bb5e41f62", "Servicios públicos", TipoMovimiento.GASTO, "bombilla", "#0ea5e9"),
            new Fila("13a77e6f-b534-438a-9e69-e7b8164e5bb7", "Salud", TipoMovimiento.GASTO, "corazon", "#6366f1"),
            new Fila("020efdd0-59c1-45c9-8d3e-9a5cc7578f5b", "Educación", TipoMovimiento.GASTO, "libro", "#3b82f6"),
            new Fila("fd75338b-1b48-4b24-aa73-e0db9f6f1304", "Entretenimiento", TipoMovimiento.GASTO, "estrella", "#e11d48"),
            new Fila("b0f172cd-4eb8-4e87-a6da-e475fea185cb", "Ropa", TipoMovimiento.GASTO, "camisa", "#a855f7"),
            new Fila("1e77b3ed-ce26-4b46-860a-19f3f7de417c", "Otros gastos", TipoMovimiento.GASTO, "puntos", "#64748b"),
            new Fila("7b49081f-0a70-49bb-8ba4-0d3391bbe5e2", "Salario", TipoMovimiento.INGRESO, "maletin", "#10b981"),
            new Fila("87aefb28-a696-420c-83d3-cc34129d3d1e", "Honorarios", TipoMovimiento.INGRESO, "documento", "#22c55e"),
            new Fila("cfbe1122-611f-4ead-b332-244752f4073a", "Inversiones", TipoMovimiento.INGRESO, "grafica", "#0d9488"),
            new Fila("6adecdf1-8aaa-460d-8f5b-2ed0165f7624", "Regalos", TipoMovimiento.INGRESO, "regalo", "#ec4899"),
            new Fila("6a6be415-ca63-494c-b4fc-ed22ba3d2b7b", "Otros ingresos", TipoMovimiento.INGRESO, "puntos", "#16a34a")
    );

    private final CategoriaRepositorioMongo mongo;
    private final IRepositorioCategorias categorias;

    public InicializadorCategorias(CategoriaRepositorioMongo mongo, IRepositorioCategorias categorias) {
        this.mongo = mongo;
        this.categorias = categorias;
    }

    @Override
    public void run(ApplicationArguments args) {
        int creadas = 0;
        for (Fila f : PREDETERMINADAS) {
            // Missing rows are created; rows from an older version get the color they lack.
            boolean falta = mongo.findById(f.id()).map(d -> !d.tieneColor()).orElse(true);
            if (falta) {
                categorias.guardar(new Categoria(UUID.fromString(f.id()), f.nombre(), f.tipo(), f.icono(), f.color(), null));
                creadas++;
            }
        }
        if (creadas > 0) {
            LOG.info("Categorías predeterminadas creadas o actualizadas: {}", creadas);
        }
    }
}
