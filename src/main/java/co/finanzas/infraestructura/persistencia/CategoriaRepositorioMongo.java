package co.finanzas.infraestructura.persistencia;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CategoriaRepositorioMongo extends MongoRepository<CategoriaDocumento, String> {
    long countByIdPropietarioIsNull();
}
