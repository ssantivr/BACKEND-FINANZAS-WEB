package co.finanzas.infraestructura.persistencia;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document("usuarios")
public class UsuarioDocumento {
    @Id
    String id;
    @Indexed(unique = true, name = "uq_usuario_correo")
    String correo;
    String nombre;
    String hashContrasena;
    String estado;
    int intentosFallidos;
    LocalDateTime bloqueadoHasta;
    LocalDateTime fechaRegistro;
    LocalDateTime eliminadaEn;
}
