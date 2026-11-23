package co.finanzas.aplicacion.puertos.salida;

import co.finanzas.aplicacion.dto.FiltroMovimientos;
import co.finanzas.aplicacion.dto.Pagina;
import co.finanzas.aplicacion.dto.TotalDiario;
import co.finanzas.aplicacion.dto.TotalPorCategoria;
import co.finanzas.dominio.modelo.Movimiento;
import co.finanzas.dominio.valor.Dinero;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IRepositorioMovimientos {
    void guardar(Movimiento m);
    void eliminar(Movimiento m);
    Optional<Movimiento> buscarDeUsuario(UUID idUsuario, UUID id);
    Pagina<Movimiento> buscar(UUID idUsuario, FiltroMovimientos filtro, int pagina, int tamano);
    /** Totals per category (both types) in the range; {@code porcentaje} is filled by the caller. */
    List<TotalPorCategoria> sumarPorCategoria(UUID idUsuario, LocalDate desde, LocalDate hasta);
    /** Totals per day and type in the range (one aggregation, at most 2 rows per day). */
    List<TotalDiario> sumarPorDia(UUID idUsuario, LocalDate desde, LocalDate hasta);
    Dinero sumarGastos(UUID idUsuario, UUID idCategoria, YearMonth periodo);
    long contarPorCategoria(UUID idUsuario, UUID idCategoria);
    int reasignarCategoria(UUID idUsuario, UUID origen, UUID destino);
}
