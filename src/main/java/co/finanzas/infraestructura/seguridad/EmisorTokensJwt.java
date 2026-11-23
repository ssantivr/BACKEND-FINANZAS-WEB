package co.finanzas.infraestructura.seguridad;

import co.finanzas.aplicacion.dto.TokensDTO;
import co.finanzas.aplicacion.puertos.salida.IEmisorTokens;
import co.finanzas.dominio.excepciones.ReglaNegocioException;
import co.finanzas.dominio.modelo.Usuario;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

/** Access token: signed JWT (15 min). Refresh and recovery tokens: random values stored only as SHA-256 hashes. */
@Component
public class EmisorTokensJwt implements IEmisorTokens {

    private static final Duration DURACION_ACCESO = Duration.ofMinutes(15);
    private static final Duration DURACION_RENOVACION = Duration.ofDays(7);
    private static final Duration DURACION_RECUPERACION = Duration.ofMinutes(30);
    /** A rotated refresh token still works once during this window (lost response, double reload). */
    static final Duration GRACIA_ROTACION = Duration.ofSeconds(30);
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final SecretKey clave;
    private final SesionRepositorioMongo sesiones;
    private final TokenRecuperacionRepositorioMongo recuperaciones;
    private final Clock reloj;

    public EmisorTokensJwt(@Value("${app.jwt.secreto}") String secreto, SesionRepositorioMongo sesiones,
                           TokenRecuperacionRepositorioMongo recuperaciones, Clock reloj) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 bytes");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.sesiones = sesiones;
        this.recuperaciones = recuperaciones;
        this.reloj = reloj;
    }

    @Override
    public TokensDTO emitir(Usuario u) {
        Instant ahora = reloj.instant();
        Instant expira = ahora.plus(DURACION_ACCESO);
        String acceso = Jwts.builder()
                .subject(u.getId().toString())
                .claim("typ", "access")
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expira))
                .signWith(clave)
                .compact();
        String refresh = aleatorio();
        SesionDocumento s = new SesionDocumento();
        s.id = UUID.randomUUID().toString();
        s.idUsuario = u.getId().toString();
        s.hash = hash(refresh);
        s.expiraEn = ahora.plus(DURACION_RENOVACION);
        sesiones.save(s);
        return new TokensDTO(acceso, refresh, expira);
    }

    @Override
    public UUID validarAcceso(String token) {
        try {
            var claims = Jwts.parser().verifyWith(clave).clock(() -> Date.from(reloj.instant())).build()
                    .parseSignedClaims(token).getPayload();
            if (!"access".equals(claims.get("typ"))) {
                throw invalido();
            }
            return UUID.fromString(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            throw invalido();
        }
    }

    @Override
    public UUID validarRefresh(String token) {
        if (token == null || token.isBlank()) {
            throw invalido();
        }
        return sesiones.findByHash(hash(token))
                .filter(s -> s.expiraEn.isAfter(reloj.instant()))
                .map(s -> UUID.fromString(s.idUsuario))
                .orElseThrow(EmisorTokensJwt::invalido);
    }

    @Override
    public void revocar(String token) {
        if (token != null && !token.isBlank()) {
            sesiones.deleteByHash(hash(token));
        }
    }

    @Override
    public void retirar(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        sesiones.findByHash(hash(token)).ifPresent(s -> {
            if (s.reemplazadaEn != null) {
                sesiones.delete(s);   // second use inside the grace period: no more uses
                return;
            }
            Instant ahora = reloj.instant();
            Instant fin = ahora.plus(GRACIA_ROTACION);
            s.reemplazadaEn = ahora;
            s.expiraEn = s.expiraEn.isBefore(fin) ? s.expiraEn : fin;
            sesiones.save(s);
        });
    }

    @Override
    public void revocarTodas(UUID idUsuario) {
        sesiones.deleteByIdUsuario(idUsuario.toString());
    }

    @Override
    public String crearTokenRecuperacion(UUID idUsuario) {
        String token = aleatorio();
        TokenRecuperacionDocumento d = new TokenRecuperacionDocumento();
        d.id = UUID.randomUUID().toString();
        d.idUsuario = idUsuario.toString();
        d.hash = hash(token);
        d.expiraEn = reloj.instant().plus(DURACION_RECUPERACION);
        d.usado = false;
        recuperaciones.save(d);
        return token;
    }

    @Override
    public UUID usarTokenRecuperacion(String token) {
        if (token == null || token.isBlank()) {
            throw enlaceInvalido();
        }
        TokenRecuperacionDocumento d = recuperaciones.findByHash(hash(token))
                .filter(t -> !t.usado && t.expiraEn.isAfter(reloj.instant()))
                .orElseThrow(EmisorTokensJwt::enlaceInvalido);
        d.usado = true;
        recuperaciones.save(d);
        return UUID.fromString(d.idUsuario);
    }

    private static String aleatorio() {
        byte[] b = new byte[32];
        ALEATORIO.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    static String hash(String valor) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ReglaNegocioException invalido() {
        return new ReglaNegocioException("TOKEN_INVALIDO", "La sesión no es válida o expiró");
    }

    private static ReglaNegocioException enlaceInvalido() {
        return new ReglaNegocioException("ENLACE_INVALIDO", "El enlace no es válido");
    }
}
