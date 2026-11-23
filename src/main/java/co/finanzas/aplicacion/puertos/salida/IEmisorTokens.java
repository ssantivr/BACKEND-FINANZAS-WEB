package co.finanzas.aplicacion.puertos.salida;

import co.finanzas.aplicacion.dto.TokensDTO;
import co.finanzas.dominio.modelo.Usuario;

import java.util.UUID;

public interface IEmisorTokens {
    TokensDTO emitir(Usuario u);
    /** @return the user id; throws ReglaNegocioException TOKEN_INVALIDO when the token is not valid. */
    UUID validarAcceso(String token);
    UUID validarRefresh(String token);
    void revocar(String token);
    /**
     * Retires a refresh token after rotation. It stays valid for one more use during a short grace period, so a
     * browser that reloads twice in a row (and loses the first response) is not signed out.
     */
    void retirar(String token);
    void revocarTodas(UUID idUsuario);
    /** One-time password-recovery token valid for 30 minutes (RF-07). */
    String crearTokenRecuperacion(UUID idUsuario);
    /** Marks the recovery token as used and returns its user; throws ENLACE_INVALIDO otherwise (RF-08). */
    UUID usarTokenRecuperacion(String token);
}
