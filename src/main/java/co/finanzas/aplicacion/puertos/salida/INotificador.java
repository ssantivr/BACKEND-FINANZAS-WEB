package co.finanzas.aplicacion.puertos.salida;

import co.finanzas.aplicacion.dto.PresupuestoDTO;
import co.finanzas.dominio.valor.Email;

public interface INotificador {
    void enviarRecuperacion(Email correo, String enlace);
    void enviarAlertaPresupuesto(Email correo, PresupuestoDTO p);

    /**
     * Whether an e-mail channel exists right now. When it does not, a budget alert is not marked as sent (RN-12),
     * so it goes out the first time a channel is available instead of being lost.
     */
    default boolean puedeEnviar() {
        return true;
    }
}
