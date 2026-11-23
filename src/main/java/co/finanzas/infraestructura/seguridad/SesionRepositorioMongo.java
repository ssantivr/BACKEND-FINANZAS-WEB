package co.finanzas.infraestructura.seguridad;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SesionRepositorioMongo extends MongoRepository<SesionDocumento, String> {
    Optional<SesionDocumento> findByHash(String hash);
    void deleteByHash(String hash);
    void deleteByIdUsuario(String idUsuario);
}
