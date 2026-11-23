package co.finanzas.aplicacion.puertos.entrada;

import co.finanzas.aplicacion.dto.TokensDTO;
import co.finanzas.aplicacion.dto.UsuarioDTO;

import java.util.UUID;

public interface IGestionCuenta {
    UsuarioDTO obtener(UUID idUsuario);
    UsuarioDTO actualizarNombre(UUID idUsuario, String nombre);
    /** Revokes every session and returns new tokens so the current device stays signed in (HU-22). */
    TokensDTO cambiarContrasena(UUID idUsuario, String actual, String nueva);
    void eliminar(UUID idUsuario);
}
