package co.finanzas.dominio.modelo;

import co.finanzas.dominio.excepciones.ReglaNegocioException;
import co.finanzas.dominio.tipos.TipoMovimiento;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Categoria {

    private final UUID id;
    private String nombre;
    private final TipoMovimiento tipo;
    private final String icono;
    /** Hex color shown next to the category (mockups HU-04 to HU-17). */
    private final String color;
    /** Empty for the predefined categories (RF-14). */
    private final UUID idPropietario;

    /** Colors assigned to user categories; contrast checked against white cards. */
    public static final List<String> PALETA = List.of(
            "#14b8a6", "#f59e0b", "#6366f1", "#e11d48", "#0ea5e9", "#a855f7", "#10b981", "#f97316", "#64748b", "#ec4899");

    public Categoria(UUID id, String nombre, TipoMovimiento tipo, String icono, UUID idPropietario) {
        this(id, nombre, tipo, icono, null, idPropietario);
    }

    public Categoria(UUID id, String nombre, TipoMovimiento tipo, String icono, String color, UUID idPropietario) {
        this.id = Objects.requireNonNull(id);
        this.nombre = validarNombre(nombre);
        this.tipo = Objects.requireNonNull(tipo);
        this.icono = icono;
        this.color = validarColor(color, id);
        this.idPropietario = idPropietario;
    }

    public static Categoria propia(String nombre, TipoMovimiento tipo, UUID idPropietario) {
        return propia(nombre, tipo, null, idPropietario);
    }

    /** {@code color} is optional: without it the category gets one from {@link #PALETA}. */
    public static Categoria propia(String nombre, TipoMovimiento tipo, String color, UUID idPropietario) {
        return new Categoria(UUID.randomUUID(), nombre, tipo, null, color, Objects.requireNonNull(idPropietario));
    }

    private static String validarColor(String color, UUID id) {
        if (color == null || color.isBlank()) {
            return PALETA.get(Math.floorMod(id.hashCode(), PALETA.size()));
        }
        if (!color.matches("#[0-9a-fA-F]{6}")) {
            throw new ReglaNegocioException("COLOR_INVALIDO", "El color debe tener el formato #RRGGBB");
        }
        return color.toLowerCase();
    }

    private static String validarNombre(String nombre) {
        String n = nombre == null ? "" : nombre.trim();
        if (n.isEmpty() || n.length() > 40) {   // RN-09
            throw new ReglaNegocioException("NOMBRE_CATEGORIA_INVALIDO", "El nombre de la categoría debe tener entre 1 y 40 caracteres");
        }
        return n;
    }

    public boolean esPredeterminada() {
        return idPropietario == null;
    }

    /** RN-04: predefined or owned by the user. */
    public boolean estaDisponiblePara(UUID idUsuario) {
        return esPredeterminada() || idPropietario.equals(idUsuario);
    }

    public boolean perteneceA(UUID idUsuario) {
        return !esPredeterminada() && idPropietario.equals(idUsuario);
    }

    public void renombrar(String nombre) {
        if (esPredeterminada()) {
            throw new ReglaNegocioException("CATEGORIA_PREDETERMINADA", "Las categorías predeterminadas no se pueden modificar");
        }
        this.nombre = validarNombre(nombre);
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public TipoMovimiento getTipo() { return tipo; }
    public String getIcono() { return icono; }
    public String getColor() { return color; }
    public UUID getIdPropietario() { return idPropietario; }
}
