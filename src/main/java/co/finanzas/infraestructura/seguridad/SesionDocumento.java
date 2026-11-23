package co.finanzas.infraestructura.seguridad;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** One refresh token. Only its SHA-256 hash is stored; MongoDB deletes it when {@code expiraEn} passes (TTL index). */
@Document("sesiones")
public class SesionDocumento {
    @Id
    String id;
    @Indexed(name = "ix_sesion_usuario")
    String idUsuario;
    @Indexed(unique = true, name = "uq_sesion_hash")
    String hash;
    @Indexed(name = "ttl_sesion", expireAfter = "0s")
    Instant expiraEn;
    /** Set when the token was rotated; it then expires after the grace period. */
    Instant reemplazadaEn;
}
