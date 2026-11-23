package co.finanzas.infraestructura.notificacion;

import co.finanzas.aplicacion.dto.PresupuestoDTO;
import co.finanzas.aplicacion.puertos.salida.INotificador;
import co.finanzas.dominio.valor.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Sends e-mails asynchronously with up to 3 attempts (RF-20), so a mail failure never undoes the operation.
 * When no SMTP server is configured (SPRING_MAIL_HOST) it only logs that the e-mail was not sent; with
 * {@code app.correo.mostrar-en-consola=true} (local profile only) it prints the whole e-mail to the console instead,
 * so password recovery and budget alerts can be tried on a developer machine without a mail server.
 */
@Component
public class NotificadorSmtp implements INotificador {

    private static final Logger LOG = LoggerFactory.getLogger(NotificadorSmtp.class);

    private final ObjectProvider<JavaMailSender> correo;
    private final String remitente;
    private final int maxReintentos = 3;
    private final boolean mostrarEnConsola;

    public NotificadorSmtp(ObjectProvider<JavaMailSender> correo,
                           @Value("${app.correo.remitente}") String remitente,
                           @Value("${app.correo.mostrar-en-consola:false}") boolean mostrarEnConsola) {
        this.correo = correo;
        this.remitente = remitente;
        this.mostrarEnConsola = mostrarEnConsola;
    }

    @Async
    @Override
    public void enviarRecuperacion(Email destino, String enlace) {
        enviar(destino, "Recupera tu contraseña - Finanzas Personales",
                "Recibimos una solicitud para restablecer tu contraseña.\n\n"
                        + "Usa este enlace (válido por 30 minutos y un solo uso):\n" + enlace + "\n\n"
                        + "Si no fuiste tú, ignora este correo.");
    }

    @Async
    @Override
    public void enviarAlertaPresupuesto(Email destino, PresupuestoDTO p) {
        enviar(destino, "Superaste tu presupuesto de " + p.categoria().nombre(),
                "En " + p.periodo() + " llevas " + p.consumo().toPlainString() + " de un límite de "
                        + p.limite().toPlainString() + " (" + p.porcentaje().toPlainString() + " %) en "
                        + p.categoria().nombre() + ".");
    }

    @Override
    public boolean puedeEnviar() {
        return mostrarEnConsola || correo.getIfAvailable() != null;
    }

    private void enviar(Email destino, String asunto, String texto) {
        JavaMailSender sender = correo.getIfAvailable();
        if (sender == null) {
            if (mostrarEnConsola) {
                LOG.info("""

                        ===== CORREO (solo desarrollo local, no se envió) =====
                        Para: {}
                        Asunto: {}

                        {}
                        ======================================================""", destino.getValor(), asunto, texto);
            } else {
                LOG.warn("SMTP no configurado: no se envió el correo «{}»", asunto);
            }
            return;
        }
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(remitente);
        m.setTo(destino.getValor());
        m.setSubject(asunto);
        m.setText(texto);
        for (int intento = 1; intento <= maxReintentos; intento++) {
            try {
                sender.send(m);
                return;
            } catch (MailException e) {
                LOG.warn("Fallo el envío de «{}» (intento {}/{}): {}", asunto, intento, maxReintentos, e.getMessage());
                dormir(1000L * intento);
            }
        }
        LOG.error("No se pudo enviar «{}» tras {} intentos", asunto, maxReintentos);
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
