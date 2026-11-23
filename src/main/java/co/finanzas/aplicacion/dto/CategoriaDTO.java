package co.finanzas.aplicacion.dto;

import co.finanzas.dominio.modelo.Categoria;
import co.finanzas.dominio.tipos.TipoMovimiento;

import java.util.UUID;

public record CategoriaDTO(UUID id, String nombre, TipoMovimiento tipo, String icono, String color, boolean predeterminada) {

    public static CategoriaDTO de(Categoria c) {
        return new CategoriaDTO(c.getId(), c.getNombre(), c.getTipo(), c.getIcono(), c.getColor(), c.esPredeterminada());
    }
}
