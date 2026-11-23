package co.finanzas.infraestructura.persistencia;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** {@code fecha} is stored as ISO text (yyyy-MM-dd): it sorts and filters correctly and has no time zone. */
@Document("movimientos")
@CompoundIndexes({
        @CompoundIndex(name = "ix_mov_usuario_fecha", def = "{'idUsuario': 1, 'fecha': -1, 'fechaRegistro': -1}"),
        @CompoundIndex(name = "ix_mov_usuario_categoria_fecha", def = "{'idUsuario': 1, 'idCategoria': 1, 'fecha': -1, 'fechaRegistro': -1}")
})
public class MovimientoDocumento {
    @Id
    String id;
    String idUsuario;
    String tipo;
    @Field(targetType = FieldType.DECIMAL128)
    BigDecimal monto;
    String moneda;
    String fecha;
    String descripcion;
    String idCategoria;
    String metodoPago;
    String fuente;
    LocalDateTime fechaRegistro;
}
