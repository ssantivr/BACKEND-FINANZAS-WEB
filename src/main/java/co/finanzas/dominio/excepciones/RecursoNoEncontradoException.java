package co.finanzas.dominio.excepciones;

import java.util.UUID;

/** The resource does not exist or belongs to another user (both answer 404, RNF-02). */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, UUID id) {
        super(recurso + " no encontrado: " + id);
    }
}
