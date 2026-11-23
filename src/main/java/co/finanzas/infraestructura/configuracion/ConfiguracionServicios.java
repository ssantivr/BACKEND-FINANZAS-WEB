package co.finanzas.infraestructura.configuracion;

import co.finanzas.aplicacion.puertos.entrada.IEvaluadorPresupuesto;
import co.finanzas.aplicacion.puertos.salida.ICifradorContrasena;
import co.finanzas.aplicacion.puertos.salida.IEmisorTokens;
import co.finanzas.aplicacion.puertos.salida.INotificador;
import co.finanzas.aplicacion.puertos.salida.IRepositorioCategorias;
import co.finanzas.aplicacion.puertos.salida.IRepositorioMovimientos;
import co.finanzas.aplicacion.puertos.salida.IRepositorioPresupuestos;
import co.finanzas.aplicacion.puertos.salida.IRepositorioUsuarios;
import co.finanzas.aplicacion.servicios.ServicioAutenticacion;
import co.finanzas.aplicacion.servicios.ServicioCategorias;
import co.finanzas.aplicacion.servicios.ServicioConsultaFinanciera;
import co.finanzas.aplicacion.servicios.ServicioCuenta;
import co.finanzas.aplicacion.servicios.ServicioMovimientos;
import co.finanzas.aplicacion.servicios.ServicioPresupuestos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Wires the application services (plain Java, no Spring annotations) with their ports. */
@Configuration
public class ConfiguracionServicios {

    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }

    @Bean
    public ServicioAutenticacion servicioAutenticacion(IRepositorioUsuarios repoUsuarios, ICifradorContrasena cifrador,
                                                       IEmisorTokens emisor, INotificador notificador, Clock reloj,
                                                       @Value("${app.frontend.url}") String urlFrontend) {
        return new ServicioAutenticacion(repoUsuarios, cifrador, emisor, notificador, reloj, urlFrontend + "/restablecer");
    }

    @Bean
    public ServicioCuenta servicioCuenta(IRepositorioUsuarios repoUsuarios, ICifradorContrasena cifrador,
                                         IEmisorTokens emisor, Clock reloj) {
        return new ServicioCuenta(repoUsuarios, cifrador, emisor, reloj);
    }

    @Bean
    public ServicioPresupuestos servicioPresupuestos(IRepositorioPresupuestos repoPresupuestos,
                                                     IRepositorioMovimientos repoMovimientos,
                                                     IRepositorioCategorias repoCategorias,
                                                     IRepositorioUsuarios repoUsuarios, INotificador notificador) {
        return new ServicioPresupuestos(repoPresupuestos, repoMovimientos, repoCategorias, repoUsuarios, notificador);
    }

    @Bean
    public ServicioMovimientos servicioMovimientos(IRepositorioMovimientos repoMovimientos,
                                                   IRepositorioCategorias repoCategorias,
                                                   IEvaluadorPresupuesto evaluador, Clock reloj) {
        return new ServicioMovimientos(repoMovimientos, repoCategorias, evaluador, reloj);
    }

    @Bean
    public ServicioCategorias servicioCategorias(IRepositorioCategorias repoCategorias,
                                                 IRepositorioMovimientos repoMovimientos,
                                                 IRepositorioPresupuestos repoPresupuestos) {
        return new ServicioCategorias(repoCategorias, repoMovimientos, repoPresupuestos);
    }

    @Bean
    public ServicioConsultaFinanciera servicioConsultaFinanciera(IRepositorioMovimientos repoMovimientos,
                                                                 IRepositorioPresupuestos repoPresupuestos, Clock reloj) {
        return new ServicioConsultaFinanciera(repoMovimientos, repoPresupuestos, reloj);
    }
}
