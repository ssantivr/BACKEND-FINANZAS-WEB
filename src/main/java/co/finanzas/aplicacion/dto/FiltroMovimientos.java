package co.finanzas.aplicacion.dto;

import co.finanzas.dominio.excepciones.ReglaNegocioException;
import co.finanzas.dominio.tipos.TipoMovimiento;

import java.time.LocalDate;
import java.util.UUID;

/** {@code texto} (optional) matches description, income source or category name, ignoring case. */
public record FiltroMovimientos(LocalDate desde, LocalDate hasta, TipoMovimiento tipo, UUID idCategoria, String texto) {

    public static final int MAX_TEXTO = 60;

    public FiltroMovimientos {
        texto = texto == null || texto.isBlank() ? null : texto.trim();
    }

    public FiltroMovimientos(LocalDate desde, LocalDate hasta, TipoMovimiento tipo, UUID idCategoria) {
        this(desde, hasta, tipo, idCategoria, null);
    }

    /** RN-13: desde <= hasta and at most 24 months. */
    public void validar() {
        if (desde == null || hasta == null) {
            throw new ReglaNegocioException("RANGO_INVALIDO", "El rango de fechas es obligatorio");
        }
        if (desde.isAfter(hasta)) {
            throw new ReglaNegocioException("RANGO_INVALIDO", "La fecha inicial no puede ser posterior a la final");
        }
        if (desde.plusMonths(24).isBefore(hasta)) {
            throw new ReglaNegocioException("RANGO_INVALIDO", "El rango no puede superar 24 meses");
        }
        if (texto != null && texto.length() > MAX_TEXTO) {
            throw new ReglaNegocioException("BUSQUEDA_INVALIDA", "La búsqueda admite máximo " + MAX_TEXTO + " caracteres");
        }
    }
}
