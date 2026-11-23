package co.finanzas.aplicacion.puertos.entrada;

import co.finanzas.aplicacion.dto.PresupuestoDTO;
import co.finanzas.dominio.valor.Dinero;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface IGestionPresupuestos {
    PresupuestoDTO definir(UUID idUsuario, UUID idCategoria, YearMonth periodo, Dinero limite);
    List<PresupuestoDTO> listar(UUID idUsuario, YearMonth periodo);
    void eliminar(UUID idUsuario, UUID idPresupuesto);
}
