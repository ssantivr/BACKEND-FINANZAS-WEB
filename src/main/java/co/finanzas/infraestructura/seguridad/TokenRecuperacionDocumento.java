package co.finanzas.infraestructura.seguridad;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("tokens_recuperacion")
public class TokenRecuperacionDocumento {
    @Id
    String id;
    String idUsuario;
    @Indexed(unique = true, name = "uq_recuperacion_hash")
    String hash;
    @Indexed(name = "ttl_recuperacion", expireAfter = "0s")
    Instant expiraEn;
    boolean usado;
}
