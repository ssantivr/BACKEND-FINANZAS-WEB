package co.finanzas.aplicacion.dto;

import co.finanzas.dominio.modelo.Usuario;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioDTO(UUID id, String nombre, String correo, LocalDateTime fechaRegistro) {

    public static UsuarioDTO de(Usuario u) {
        return new UsuarioDTO(u.getId(), u.getNombre(), u.getCorreo().getValor(), u.getFechaRegistro());
    }
}
