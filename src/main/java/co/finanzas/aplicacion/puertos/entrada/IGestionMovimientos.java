package co.finanzas.aplicacion.puertos.entrada;

import co.finanzas.aplicacion.dto.ModificarMovimientoCmd;
import co.finanzas.aplicacion.dto.MovimientoDTO;
import co.finanzas.aplicacion.dto.RegistrarGastoCmd;
import co.finanzas.aplicacion.dto.RegistrarIngresoCmd;

import java.util.UUID;

public interface IGestionMovimientos {
    MovimientoDTO registrarGasto(RegistrarGastoCmd cmd);
    MovimientoDTO registrarIngreso(RegistrarIngresoCmd cmd);
    MovimientoDTO modificar(UUID idUsuario, UUID idMovimiento, ModificarMovimientoCmd cmd);
    void eliminar(UUID idUsuario, UUID idMovimiento);
}
