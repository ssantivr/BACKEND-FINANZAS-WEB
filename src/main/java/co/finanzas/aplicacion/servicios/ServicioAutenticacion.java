package co.finanzas.aplicacion.servicios;

import co.finanzas.aplicacion.dto.RegistroCmd;
import co.finanzas.aplicacion.dto.TokensDTO;
import co.finanzas.aplicacion.dto.UsuarioDTO;
import co.finanzas.aplicacion.puertos.entrada.IAutenticacion;
import co.finanzas.aplicacion.puertos.salida.ICifradorContrasena;
import co.finanzas.aplicacion.puertos.salida.IEmisorTokens;
import co.finanzas.aplicacion.puertos.salida.INotificador;
import co.finanzas.aplicacion.puertos.salida.IRepositorioUsuarios;
import co.finanzas.dominio.excepciones.ReglaNegocioException;
import co.finanzas.dominio.modelo.Usuario;
import co.finanzas.dominio.valor.Email;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class ServicioAutenticacion implements IAutenticacion {

    private final IRepositorioUsuarios repoUsuarios;
    private final ICifradorContrasena cifrador;
    private final IEmisorTokens emisor;
    private final INotificador notificador;
    private final Clock reloj;
    private final String urlRestablecer;

    public ServicioAutenticacion(IRepositorioUsuarios repoUsuarios, ICifradorContrasena cifrador, IEmisorTokens emisor,
                                 INotificador notificador, Clock reloj, String urlRestablecer) {
        this.repoUsuarios = repoUsuarios;
        this.cifrador = cifrador;
        this.emisor = emisor;
        this.notificador = notificador;
        this.reloj = reloj;
        this.urlRestablecer = urlRestablecer;
    }

    @Override
    public UsuarioDTO registrar(RegistroCmd cmd) {
        Email correo = new Email(cmd.correo());
        Validaciones.validarClave(cmd.clave());
        if (repoUsuarios.existeCorreo(correo)) {
            throw new ReglaNegocioException("CORREO_DUPLICADO", "Este correo ya está registrado");
        }
        Usuario u = new Usuario(cmd.nombre(), correo, cifrador.cifrar(cmd.clave()), LocalDateTime.now(reloj));
        repoUsuarios.guardar(u);
        return UsuarioDTO.de(u);
    }

    @Override
    public TokensDTO iniciarSesion(String correo, String clave) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Usuario u = buscarActivo(correo).orElseThrow(ServicioAutenticacion::credencialesInvalidas);
        if (u.estaBloqueado(ahora)) {
            throw cuentaBloqueada();
        }
        if (clave == null || !cifrador.coincide(clave, u.getHashContrasena())) {
            u.registrarIntentoFallido(ahora);
            repoUsuarios.guardar(u);
            throw u.estaBloqueado(ahora) ? cuentaBloqueada() : credencialesInvalidas();
        }
        u.reiniciarIntentos();
        repoUsuarios.guardar(u);
        return emisor.emitir(u);
    }

    @Override
    public TokensDTO renovarSesion(String refreshToken) {
        UUID id = emisor.validarRefresh(refreshToken);
        emisor.retirar(refreshToken);   // rotation: the old token gets one more use for a few seconds
        Usuario u = repoUsuarios.buscarPorId(id).filter(Usuario::estaActiva)
                .orElseThrow(() -> new ReglaNegocioException("TOKEN_INVALIDO", "La sesión no es válida"));
        return emisor.emitir(u);
    }

    @Override
    public void cerrarSesion(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            emisor.revocar(refreshToken);
        }
    }

    @Override
    public void solicitarRecuperacion(String correo) {
        // Same answer whether the address exists or not (HU-05), so nothing is returned or thrown here.
        Optional<Usuario> u;
        try {
            u = buscarActivo(correo);
        } catch (ReglaNegocioException e) {
            return;
        }
        u.ifPresent(usuario -> {
            String token = emisor.crearTokenRecuperacion(usuario.getId());
            notificador.enviarRecuperacion(usuario.getCorreo(), urlRestablecer + "?token=" + token);
        });
    }

    @Override
    public void restablecerContrasena(String token, String nuevaClave) {
        Validaciones.validarClave(nuevaClave);
        UUID id = emisor.usarTokenRecuperacion(token);
        Usuario u = repoUsuarios.buscarPorId(id).filter(Usuario::estaActiva)
                .orElseThrow(() -> new ReglaNegocioException("ENLACE_INVALIDO", "El enlace no es válido"));
        u.cambiarContrasena(cifrador.cifrar(nuevaClave));
        u.reiniciarIntentos();
        repoUsuarios.guardar(u);
        emisor.revocarTodas(id);
    }

    private Optional<Usuario> buscarActivo(String correo) {
        if (correo == null || correo.isBlank()) {
            return Optional.empty();
        }
        return repoUsuarios.buscarPorCorreo(new Email(correo)).filter(Usuario::estaActiva);
    }

    private static ReglaNegocioException credencialesInvalidas() {
        return new ReglaNegocioException("CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos");
    }

    private static ReglaNegocioException cuentaBloqueada() {
        return new ReglaNegocioException("CUENTA_BLOQUEADA",
                "La cuenta está bloqueada por 15 minutos por intentos fallidos");
    }
}
