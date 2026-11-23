package co.finanzas.infraestructura.persistencia;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/** {@code nombreClave} is the lower-case name used by the unique index (RN-09). */
@Document("categorias")
@CompoundIndex(name = "uq_categoria_nombre", def = "{'idPropietario': 1, 'tipo': 1, 'nombreClave': 1}", unique = true)
public class CategoriaDocumento {
    @Id
    String id;
    String nombre;
    String nombreClave;
    String tipo;
    String icono;
    String color;
    /** null for predefined categories. */
    String idPropietario;

    /** Rows saved before the color field existed return false. */
    public boolean tieneColor() {
        return color != null;
    }
}
