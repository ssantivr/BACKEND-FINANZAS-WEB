package co.finanzas.dominio.excepciones;

/** A business rule was violated. {@code codigo} is a stable identifier the web layer maps to an HTTP status. */
public class ReglaNegocioException extends RuntimeException {

    private final String codigo;

    public ReglaNegocioException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
