package co.finanzas.infraestructura.web;

import co.finanzas.aplicacion.dto.TokensDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/** Builds the HttpOnly refresh-token cookie. Cross-site (Vercel -> Render) needs SameSite=None and Secure. */
@Component
public class CookieSesion {

    public static final String NOMBRE = "refresh_token";
    private static final String RUTA = "/api/v1/auth";

    private final boolean segura;
    private final String sameSite;

    public CookieSesion(@Value("${app.cookie.segura}") boolean segura, @Value("${app.cookie.same-site}") String sameSite) {
        this.segura = segura;
        this.sameSite = sameSite;
    }

    public String crear(TokensDTO t) {
        return ResponseCookie.from(NOMBRE, t.refreshToken()).httpOnly(true).secure(segura).sameSite(sameSite)
                .path(RUTA).maxAge(Duration.ofDays(7)).build().toString();
    }

    public String borrar() {
        return ResponseCookie.from(NOMBRE, "").httpOnly(true).secure(segura).sameSite(sameSite)
                .path(RUTA).maxAge(0).build().toString();
    }

    /** Body returned to the client: never contains the refresh token. */
    public record SesionResponse(String accessToken, Instant expiraEn) {
        public static SesionResponse de(TokensDTO t) {
            return new SesionResponse(t.accessToken(), t.expiraEn());
        }
    }
}
