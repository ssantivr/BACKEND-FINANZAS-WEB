package co.finanzas.infraestructura.seguridad;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TokenRecuperacionRepositorioMongo extends MongoRepository<TokenRecuperacionDocumento, String> {
    Optional<TokenRecuperacionDocumento> findByHash(String hash);
}
