package co.finanzas.aplicacion.puertos.entrada;

import co.finanzas.aplicacion.dto.RegistroCmd;
import co.finanzas.aplicacion.dto.TokensDTO;
import co.finanzas.aplicacion.dto.UsuarioDTO;

public interface IAutenticacion {
    UsuarioDTO registrar(RegistroCmd cmd);
    TokensDTO iniciarSesion(String correo, String clave);
    TokensDTO renovarSesion(String refreshToken);
    void cerrarSesion(String refreshToken);
    void solicitarRecuperacion(String correo);
    void restablecerContrasena(String token, String nuevaClave);
}
