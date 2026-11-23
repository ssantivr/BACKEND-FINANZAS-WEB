package co.finanzas.aplicacion.servicios;

import co.finanzas.aplicacion.dto.TokensDTO;
import co.finanzas.aplicacion.dto.UsuarioDTO;
import co.finanzas.aplicacion.puertos.entrada.IGestionCuenta;
import co.finanzas.aplicacion.puertos.salida.ICifradorContrasena;
import co.finanzas.aplicacion.puertos.salida.IEmisorTokens;
import co.finanzas.aplicacion.puertos.salida.IRepositorioUsuarios;
import co.finanzas.dominio.excepciones.RecursoNoEncontradoException;
import co.finanzas.dominio.excepciones.ReglaNegocioException;
import co.finanzas.dominio.modelo.Usuario;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

public class ServicioCuenta implements IGestionCuenta {

    private final IRepositorioUsuarios repoUsuarios;
    private final ICifradorContrasena cifrador;
    private final IEmisorTokens emisor;
    private final Clock reloj;

    public ServicioCuenta(IRepositorioUsuarios repoUsuarios, ICifradorContrasena cifrador, IEmisorTokens emisor,
                          Clock reloj) {
        this.repoUsuarios = repoUsuarios;
        this.cifrador = cifrador;
        this.emisor = emisor;
        this.reloj = reloj;
    }

    @Override
    public UsuarioDTO obtener(UUID idUsuario) {
        return UsuarioDTO.de(activo(idUsuario));
    }

    @Override
    public UsuarioDTO actualizarNombre(UUID idUsuario, String nombre) {
        Usuario u = activo(idUsuario);
        u.cambiarNombre(nombre);
        repoUsuarios.guardar(u);
        return UsuarioDTO.de(u);
    }

    @Override
    public TokensDTO cambiarContrasena(UUID idUsuario, String actual, String nueva) {
        Usuario u = activo(idUsuario);
        if (actual == null || !cifrador.coincide(actual, u.getHashContrasena())) {
            throw new ReglaNegocioException("CONTRASENA_ACTUAL_INCORRECTA", "La contraseña actual no es correcta");
        }
        Validaciones.validarClave(nueva);
        if (nueva.equals(actual)) {
            throw new ReglaNegocioException("CLAVE_REPETIDA", "La nueva contraseña debe ser distinta de la actual");
        }
        u.cambiarContrasena(cifrador.cifrar(nueva));
        repoUsuarios.guardar(u);
        emisor.revocarTodas(idUsuario);
        return emisor.emitir(u);
    }

    @Override
    public void eliminar(UUID idUsuario) {
        Usuario u = activo(idUsuario);
        u.eliminar(LocalDateTime.now(reloj));
        repoUsuarios.guardar(u);
        emisor.revocarTodas(idUsuario);
    }

    private Usuario activo(UUID idUsuario) {
        return repoUsuarios.buscarPorId(idUsuario).filter(Usuario::estaActiva)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", idUsuario));
    }
}
