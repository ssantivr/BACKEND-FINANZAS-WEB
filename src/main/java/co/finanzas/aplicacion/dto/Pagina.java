package co.finanzas.aplicacion.dto;

import java.util.List;
import java.util.function.Function;

public record Pagina<T>(List<T> elementos, int pagina, int tamano, long totalElementos) {

    public <R> Pagina<R> map(Function<T, R> f) {
        return new Pagina<>(elementos.stream().map(f).toList(), pagina, tamano, totalElementos);
    }
}
