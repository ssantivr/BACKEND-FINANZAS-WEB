package co.finanzas.aplicacion.puertos.salida;

import co.finanzas.dominio.modelo.Usuario;
import co.finanzas.dominio.valor.Email;

import java.util.Optional;
import java.util.UUID;

public interface IRepositorioUsuarios {
    void guardar(Usuario u);
    Optional<Usuario> buscarPorId(UUID id);
    Optional<Usuario> buscarPorCorreo(Email correo);
    boolean existeCorreo(Email correo);
}
