package co.finanzas.infraestructura.persistencia;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UsuarioRepositorioMongo extends MongoRepository<UsuarioDocumento, String> {
    Optional<UsuarioDocumento> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
}
