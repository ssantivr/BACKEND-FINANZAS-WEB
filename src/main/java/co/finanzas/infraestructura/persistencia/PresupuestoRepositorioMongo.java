package co.finanzas.infraestructura.persistencia;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PresupuestoRepositorioMongo extends MongoRepository<PresupuestoDocumento, String> {
    Optional<PresupuestoDocumento> findByIdUsuarioAndIdCategoriaAndPeriodo(String idUsuario, String idCategoria, String periodo);
    Optional<PresupuestoDocumento> findByIdAndIdUsuario(String id, String idUsuario);
    List<PresupuestoDocumento> findByIdUsuarioAndPeriodo(String idUsuario, String periodo);
    void deleteByIdUsuarioAndIdCategoria(String idUsuario, String idCategoria);
}
