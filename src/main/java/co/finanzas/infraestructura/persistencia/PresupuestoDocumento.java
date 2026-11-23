package co.finanzas.infraestructura.persistencia;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

@Document("presupuestos")
@CompoundIndexes({
        @CompoundIndex(name = "uq_presupuesto", def = "{'idUsuario': 1, 'idCategoria': 1, 'periodo': 1}", unique = true),
        @CompoundIndex(name = "ix_presupuesto_periodo", def = "{'idUsuario': 1, 'periodo': 1}")
})
public class PresupuestoDocumento {
    @Id
    String id;
    String idUsuario;
    String idCategoria;
    /** yyyy-MM */
    String periodo;
    @Field(targetType = FieldType.DECIMAL128)
    BigDecimal limite;
    String moneda;
    boolean notificado;
}
