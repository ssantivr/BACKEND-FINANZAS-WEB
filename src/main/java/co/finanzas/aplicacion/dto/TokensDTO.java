package co.finanzas.aplicacion.dto;

import java.time.Instant;

/** The refresh token travels only in an HttpOnly cookie; the web layer never puts it in a response body. */
public record TokensDTO(String accessToken, String refreshToken, Instant expiraEn) {
}
