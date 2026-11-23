package co.finanzas.dominio.valor;

import co.finanzas.dominio.excepciones.ReglaNegocioException;

import java.util.Locale;
import java.util.regex.Pattern;

/** E-mail value object, normalized to lower case (RN-06). */
public record Email(String valor) {

    private static final Pattern FORMATO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");

    public Email {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException("CORREO_INVALIDO", "El correo es obligatorio");
        }
        valor = valor.trim().toLowerCase(Locale.ROOT);
        if (valor.length() > 254 || !FORMATO.matcher(valor).matches()) {
            throw new ReglaNegocioException("CORREO_INVALIDO", "El correo no tiene un formato válido");
        }
    }

    public String getValor() {
        return valor;
    }

    @Override
    public String toString() {
        return valor;
    }
}
