package co.finanzas.aplicacion.servicios;

import co.finanzas.dominio.excepciones.ReglaNegocioException;

final class Validaciones {

    static final int MIN_CLAVE = 10;   // RN-07

    private Validaciones() {
    }

    static void validarClave(String clave) {
        if (clave == null || clave.length() < MIN_CLAVE) {
            throw new ReglaNegocioException("CLAVE_CORTA", "La contraseña debe tener al menos 10 caracteres");
        }
        if (clave.length() > 72) {   // BCrypt only uses the first 72 bytes
            throw new ReglaNegocioException("CLAVE_LARGA", "La contraseña admite como máximo 72 caracteres");
        }
    }
}
