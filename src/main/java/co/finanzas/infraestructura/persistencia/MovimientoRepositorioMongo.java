package co.finanzas.infraestructura.persistencia;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MovimientoRepositorioMongo extends MongoRepository<MovimientoDocumento, String> {
    Optional<MovimientoDocumento> findByIdAndIdUsuario(String id, String idUsuario);
    long countByIdUsuarioAndIdCategoria(String idUsuario, String idCategoria);
}
