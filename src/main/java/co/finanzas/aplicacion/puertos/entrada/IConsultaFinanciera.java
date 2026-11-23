package co.finanzas.aplicacion.puertos.entrada;

import co.finanzas.aplicacion.dto.FiltroMovimientos;
import co.finanzas.aplicacion.dto.MovimientoDTO;
import co.finanzas.aplicacion.dto.Pagina;
import co.finanzas.aplicacion.dto.ResumenFinanciero;

import java.time.LocalDate;
import java.util.UUID;

public interface IConsultaFinanciera {
    Pagina<MovimientoDTO> consultarHistorial(UUID idUsuario, FiltroMovimientos filtro, int pagina, int tamano);
    MovimientoDTO obtenerMovimiento(UUID idUsuario, UUID idMovimiento);
    ResumenFinanciero obtenerResumen(UUID idUsuario, LocalDate desde, LocalDate hasta);
}
