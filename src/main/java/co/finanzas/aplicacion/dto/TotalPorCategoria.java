package co.finanzas.aplicacion.dto;

import java.math.BigDecimal;

public record TotalPorCategoria(CategoriaDTO categoria, BigDecimal total, BigDecimal porcentaje) {
}
