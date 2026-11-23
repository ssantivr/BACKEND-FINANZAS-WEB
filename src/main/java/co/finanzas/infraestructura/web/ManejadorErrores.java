package co.finanzas.infraestructura.web;

import co.finanzas.dominio.excepciones.RecursoNoEncontradoException;
import co.finanzas.dominio.excepciones.ReglaNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.stream.Collectors;

/** Every error is a Problem Details body (RFC 9457) in Spanish, with a stable {@code codigo}. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

    private static final Map<String, HttpStatus> ESTADOS = Map.ofEntries(
            Map.entry("CORREO_DUPLICADO", HttpStatus.CONFLICT),
            Map.entry("CATEGORIA_DUPLICADA", HttpStatus.CONFLICT),
            Map.entry("CREDENCIALES_INVALIDAS", HttpStatus.UNAUTHORIZED),
            Map.entry("TOKEN_INVALIDO", HttpStatus.UNAUTHORIZED),
            Map.entry("ORIGEN_NO_PERMITIDO", HttpStatus.FORBIDDEN),
            Map.entry("CUENTA_BLOQUEADA", HttpStatus.LOCKED),
            Map.entry("ENLACE_INVALIDO", HttpStatus.BAD_REQUEST),
            Map.entry("CLAVE_CORTA", HttpStatus.BAD_REQUEST),
            Map.entry("CLAVE_LARGA", HttpStatus.BAD_REQUEST),
            Map.entry("RANGO_INVALIDO", HttpStatus.BAD_REQUEST),
            Map.entry("PAGINA_INVALIDA", HttpStatus.BAD_REQUEST));

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ProblemDetail> reglaNegocio(ReglaNegocioException e) {
        HttpStatus estado = ESTADOS.getOrDefault(e.getCodigo(), HttpStatus.UNPROCESSABLE_ENTITY);
        return respuesta(estado, e.getMessage(), e.getCodigo());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ProblemDetail> noEncontrado(RecursoNoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, "El recurso no existe", "NO_ENCONTRADO");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validacion(MethodArgumentNotValidException e) {
        String campos = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).collect(Collectors.joining("; "));
        return respuesta(HttpStatus.BAD_REQUEST, "Datos inválidos: " + campos, "DATOS_INVALIDOS");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ProblemDetail> formato(Exception e) {
        return respuesta(HttpStatus.BAD_REQUEST, "La petición tiene un formato inválido", "FORMATO_INVALIDO");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> rutaInexistente(NoResourceFoundException e) {
        return respuesta(HttpStatus.NOT_FOUND, "La ruta no existe", "NO_ENCONTRADO");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> inesperado(Exception e) {
        LOG.error("Error no controlado", e);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado", "ERROR_INTERNO");
    }

    private static ResponseEntity<ProblemDetail> respuesta(HttpStatus estado, String detalle, String codigo) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(estado, detalle);
        p.setTitle(estado.getReasonPhrase());
        p.setProperty("codigo", codigo);
        return ResponseEntity.status(estado).body(p);
    }
}
