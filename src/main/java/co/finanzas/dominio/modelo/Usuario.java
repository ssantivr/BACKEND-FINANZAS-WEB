package co.finanzas.dominio.modelo;

import co.finanzas.dominio.excepciones.ReglaNegocioException;
import co.finanzas.dominio.tipos.EstadoCuenta;
import co.finanzas.dominio.valor.Email;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Usuario {

    public static final int MAX_INTENTOS = 5;                       // RN-08
    public static final Duration BLOQUEO = Duration.ofMinutes(15);  // RN-08

    private final UUID id;
    private String nombre;
    private final Email correo;
    private String hashContrasena;
    private EstadoCuenta estado;
    private int intentosFallidos;
    private LocalDateTime bloqueadoHasta;
    private final LocalDateTime fechaRegistro;
    private LocalDateTime eliminadaEn;

    public Usuario(String nombre, Email correo, String hashContrasena, LocalDateTime ahora) {
        this(UUID.randomUUID(), nombre, correo, hashContrasena, EstadoCuenta.ACTIVA, 0, null, ahora, null);
    }

    /** Rebuilds a stored user. */
    public Usuario(UUID id, String nombre, Email correo, String hashContrasena, EstadoCuenta estado,
                   int intentosFallidos, LocalDateTime bloqueadoHasta, LocalDateTime fechaRegistro,
                   LocalDateTime eliminadaEn) {
        this.id = Objects.requireNonNull(id);
        this.nombre = validarNombre(nombre);
        this.correo = Objects.requireNonNull(correo);
        this.hashContrasena = Objects.requireNonNull(hashContrasena);
        this.estado = Objects.requireNonNull(estado);
        this.intentosFallidos = intentosFallidos;
        this.bloqueadoHasta = bloqueadoHasta;
        this.fechaRegistro = Objects.requireNonNull(fechaRegistro);
        this.eliminadaEn = eliminadaEn;
    }

    private static String validarNombre(String nombre) {
        String n = nombre == null ? "" : nombre.trim();
        if (n.length() < 2 || n.length() > 80) {
            throw new ReglaNegocioException("NOMBRE_INVALIDO", "El nombre debe tener entre 2 y 80 caracteres");
        }
        return n;
    }

    public void cambiarContrasena(String nuevoHash) {
        this.hashContrasena = Objects.requireNonNull(nuevoHash);
    }

    public void cambiarNombre(String nombre) {
        this.nombre = validarNombre(nombre);
    }

    /** RN-08: the fifth consecutive failure locks the account for 15 minutes. */
    public void registrarIntentoFallido(LocalDateTime ahora) {
        intentosFallidos++;
        if (intentosFallidos >= MAX_INTENTOS) {
            bloqueadoHasta = ahora.plus(BLOQUEO);
            intentosFallidos = 0;
        }
    }

    public void reiniciarIntentos() {
        intentosFallidos = 0;
        bloqueadoHasta = null;
    }

    public boolean estaBloqueado(LocalDateTime ahora) {
        return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
    }

    /** Logical delete (D-1): the account can no longer be used. */
    public void eliminar(LocalDateTime ahora) {
        estado = EstadoCuenta.ELIMINADA;
        eliminadaEn = ahora;
    }

    public boolean estaActiva() {
        return estado == EstadoCuenta.ACTIVA;
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public Email getCorreo() { return correo; }
    public String getHashContrasena() { return hashContrasena; }
    public EstadoCuenta getEstado() { return estado; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public LocalDateTime getEliminadaEn() { return eliminadaEn; }
}
