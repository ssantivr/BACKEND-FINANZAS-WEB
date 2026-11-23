package co.finanzas.infraestructura.web;

import co.finanzas.aplicacion.dto.TokensDTO;
import co.finanzas.aplicacion.dto.UsuarioDTO;
import co.finanzas.aplicacion.puertos.entrada.IGestionCuenta;
import co.finanzas.infraestructura.web.CookieSesion.SesionResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** RF-25 to RF-27 (HU-20 to HU-22). */
@RestController
@RequestMapping("/api/v1/cuenta")
public class CuentaController {

    public record NombreRequest(@NotBlank String nombre) {
    }

    public record ContrasenaRequest(@NotBlank String actual, @NotBlank String nueva) {
    }

    private final IGestionCuenta gestionCuenta;
    private final CookieSesion cookie;

    public CuentaController(IGestionCuenta gestionCuenta, CookieSesion cookie) {
        this.gestionCuenta = gestionCuenta;
        this.cookie = cookie;
    }

    @GetMapping
    public UsuarioDTO obtener(@AuthenticationPrincipal UUID idUsuario) {
        return gestionCuenta.obtener(idUsuario);
    }

    @PatchMapping
    public UsuarioDTO actualizarNombre(@AuthenticationPrincipal UUID idUsuario, @Valid @RequestBody NombreRequest r) {
        return gestionCuenta.actualizarNombre(idUsuario, r.nombre());
    }

    @PutMapping("/contrasena")
    public ResponseEntity<SesionResponse> cambiarContrasena(@AuthenticationPrincipal UUID idUsuario,
                                                            @Valid @RequestBody ContrasenaRequest r) {
        TokensDTO t = gestionCuenta.cambiarContrasena(idUsuario, r.actual(), r.nueva());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.crear(t)).body(SesionResponse.de(t));
    }

    @DeleteMapping
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal UUID idUsuario) {
        gestionCuenta.eliminar(idUsuario);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.borrar()).build();
    }
}
