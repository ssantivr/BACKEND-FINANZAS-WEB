package co.finanzas.infraestructura.web;

import co.finanzas.aplicacion.dto.CategoriaDTO;
import co.finanzas.aplicacion.puertos.entrada.IGestionCategorias;
import co.finanzas.dominio.tipos.TipoMovimiento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** RF-14 to RF-17. */
@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaController {

    public record CrearCategoriaRequest(@NotBlank String nombre, @NotNull TipoMovimiento tipo, String color) {
    }

    public record RenombrarRequest(@NotBlank String nombre) {
    }

    private final IGestionCategorias gestion;

    public CategoriaController(IGestionCategorias gestion) {
        this.gestion = gestion;
    }

    @GetMapping
    public List<CategoriaDTO> listar(@AuthenticationPrincipal UUID idUsuario,
                                     @RequestParam(required = false) TipoMovimiento tipo) {
        return gestion.listarDisponibles(idUsuario, tipo);
    }

    @PostMapping
    public ResponseEntity<CategoriaDTO> crear(@AuthenticationPrincipal UUID idUsuario,
                                              @Valid @RequestBody CrearCategoriaRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gestion.crear(idUsuario, r.nombre(), r.tipo(), r.color()));
    }

    @PatchMapping("/{id}")
    public CategoriaDTO renombrar(@AuthenticationPrincipal UUID idUsuario, @PathVariable UUID id,
                                  @Valid @RequestBody RenombrarRequest r) {
        return gestion.renombrar(idUsuario, id, r.nombre());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal UUID idUsuario, @PathVariable UUID id,
                                         @RequestParam(required = false) UUID destino) {
        gestion.eliminar(idUsuario, id, destino);
        return ResponseEntity.noContent().build();
    }
}
