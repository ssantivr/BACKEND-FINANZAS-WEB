package co.finanzas.infraestructura.web;

import co.finanzas.aplicacion.dto.FiltroMovimientos;
import co.finanzas.aplicacion.dto.MovimientoDTO;
import co.finanzas.aplicacion.dto.Pagina;
import co.finanzas.aplicacion.dto.ResumenFinanciero;
import co.finanzas.aplicacion.puertos.entrada.IConsultaFinanciera;
import co.finanzas.dominio.tipos.TipoMovimiento;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/** RF-21 to RF-24. Without dates it uses the current month (HU-16). */
@RestController
@RequestMapping("/api/v1")
public class ConsultaController {

    private final IConsultaFinanciera consulta;
    private final Clock reloj;

    public ConsultaController(IConsultaFinanciera consulta, Clock reloj) {
        this.consulta = consulta;
        this.reloj = reloj;
    }

    @GetMapping("/movimientos")
    public Pagina<MovimientoDTO> historial(@AuthenticationPrincipal UUID idUsuario,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                           @RequestParam(required = false) TipoMovimiento tipo,
                                           @RequestParam(required = false) UUID categoria,
                                           @RequestParam(required = false) String texto,
                                           @RequestParam(defaultValue = "0") int pagina,
                                           @RequestParam(defaultValue = "20") int tamano) {
        YearMonth mes = YearMonth.now(reloj);
        FiltroMovimientos f = new FiltroMovimientos(desde == null ? mes.atDay(1) : desde,
                hasta == null ? mes.atEndOfMonth() : hasta, tipo, categoria, texto);
        return consulta.consultarHistorial(idUsuario, f, pagina, tamano);
    }

    @GetMapping("/movimientos/{id}")
    public MovimientoDTO obtenerMovimiento(@AuthenticationPrincipal UUID idUsuario, @PathVariable UUID id) {
        return consulta.obtenerMovimiento(idUsuario, id);
    }

    @GetMapping("/resumen")
    public ResumenFinanciero resumen(@AuthenticationPrincipal UUID idUsuario,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        YearMonth mes = YearMonth.now(reloj);
        return consulta.obtenerResumen(idUsuario, desde == null ? mes.atDay(1) : desde,
                hasta == null ? mes.atEndOfMonth() : hasta);
    }
}
