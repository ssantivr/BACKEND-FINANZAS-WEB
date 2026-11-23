package co.finanzas.aplicacion;

import co.finanzas.aplicacion.dto.CategoriaDTO;
import co.finanzas.aplicacion.dto.FiltroMovimientos;
import co.finanzas.aplicacion.dto.Pagina;
import co.finanzas.aplicacion.dto.PresupuestoDTO;
import co.finanzas.aplicacion.dto.TokensDTO;
import co.finanzas.aplicacion.dto.TotalDiario;
import co.finanzas.aplicacion.dto.TotalPorCategoria;
import co.finanzas.aplicacion.puertos.salida.ICifradorContrasena;
import co.finanzas.aplicacion.puertos.salida.IEmisorTokens;
import co.finanzas.aplicacion.puertos.salida.INotificador;
import co.finanzas.aplicacion.puertos.salida.IRepositorioCategorias;
import co.finanzas.aplicacion.puertos.salida.IRepositorioMovimientos;
import co.finanzas.aplicacion.puertos.salida.IRepositorioPresupuestos;
import co.finanzas.aplicacion.puertos.salida.IRepositorioUsuarios;
import co.finanzas.dominio.excepciones.ReglaNegocioException;
import co.finanzas.dominio.modelo.Categoria;
import co.finanzas.dominio.modelo.Gasto;
import co.finanzas.dominio.modelo.Ingreso;
import co.finanzas.dominio.modelo.Movimiento;
import co.finanzas.dominio.modelo.Presupuesto;
import co.finanzas.dominio.modelo.Usuario;
import co.finanzas.dominio.tipos.TipoMovimiento;
import co.finanzas.dominio.valor.Dinero;
import co.finanzas.dominio.valor.Email;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** In-memory adapters for the output ports: the services are tested without MongoDB or Spring. */
public final class Memoria {

    private Memoria() {
    }

    public static class Usuarios implements IRepositorioUsuarios {
        public final Map<UUID, Usuario> datos = new HashMap<>();
        public void guardar(Usuario u) { datos.put(u.getId(), u); }
        public Optional<Usuario> buscarPorId(UUID id) { return Optional.ofNullable(datos.get(id)); }
        public Optional<Usuario> buscarPorCorreo(Email c) {
            return datos.values().stream().filter(u -> u.getCorreo().equals(c)).findFirst();
        }
        public boolean existeCorreo(Email c) { return buscarPorCorreo(c).isPresent(); }
    }

    public static class Categorias implements IRepositorioCategorias {
        public final Map<UUID, Categoria> datos = new LinkedHashMap<>();
        public void guardar(Categoria c) {
            boolean dup = datos.values().stream().anyMatch(o -> !o.getId().equals(c.getId())
                    && o.getTipo() == c.getTipo() && o.getNombre().equalsIgnoreCase(c.getNombre())
                    && java.util.Objects.equals(o.getIdPropietario(), c.getIdPropietario()));
            if (dup) {
                throw new ReglaNegocioException("CATEGORIA_DUPLICADA", "Ya existe una categoría con ese nombre");
            }
            datos.put(c.getId(), c);
        }
        public void eliminar(Categoria c) { datos.remove(c.getId()); }
        public Optional<Categoria> buscarDisponible(UUID id, UUID u) {
            return Optional.ofNullable(datos.get(id)).filter(c -> c.estaDisponiblePara(u));
        }
        public List<Categoria> listarDisponibles(UUID u, TipoMovimiento t) {
            return datos.values().stream().filter(c -> c.estaDisponiblePara(u) && (t == null || c.getTipo() == t)).toList();
        }
    }

    public static class Movimientos implements IRepositorioMovimientos {
        public final Map<UUID, Movimiento> datos = new LinkedHashMap<>();
        private final Categorias categorias;
        public Movimientos(Categorias categorias) { this.categorias = categorias; }
        public void guardar(Movimiento m) { datos.put(m.getId(), m); }
        public void eliminar(Movimiento m) { datos.remove(m.getId()); }
        public Optional<Movimiento> buscarDeUsuario(UUID u, UUID id) {
            return Optional.ofNullable(datos.get(id)).filter(m -> m.perteneceA(u));
        }
        public Pagina<Movimiento> buscar(UUID u, FiltroMovimientos f, int pagina, int tamano) {
            List<Movimiento> todos = datos.values().stream().filter(m -> m.perteneceA(u))
                    .filter(m -> !m.getFecha().isBefore(f.desde()) && !m.getFecha().isAfter(f.hasta()))
                    .filter(m -> f.tipo() == null || m.getTipo() == f.tipo())
                    .filter(m -> f.idCategoria() == null || m.getCategoria().getId().equals(f.idCategoria()))
                    .filter(m -> f.texto() == null || coincide(m, f.texto().toLowerCase()))
                    .sorted(Comparator.comparing(Movimiento::getFecha).reversed()).toList();
            List<Movimiento> pag = todos.stream().skip((long) pagina * tamano).limit(tamano).toList();
            return new Pagina<>(pag, pagina, tamano, todos.size());
        }
        private static boolean coincide(Movimiento m, String t) {
            String fuente = m instanceof co.finanzas.dominio.modelo.Ingreso i ? i.getFuente() : null;
            return java.util.stream.Stream.of(m.getDescripcion(), fuente, m.getCategoria().getNombre())
                    .anyMatch(v -> v != null && v.toLowerCase().contains(t));
        }
        public List<TotalPorCategoria> sumarPorCategoria(UUID u, LocalDate desde, LocalDate hasta) {
            Map<UUID, Dinero> suma = new LinkedHashMap<>();
            Map<UUID, Categoria> cats = new HashMap<>();
            datos.values().stream().filter(m -> m.perteneceA(u))
                    .filter(m -> !m.getFecha().isBefore(desde) && !m.getFecha().isAfter(hasta))
                    .forEach(m -> {
                        cats.put(m.getCategoria().getId(), m.getCategoria());
                        suma.merge(m.getCategoria().getId(), m.getMonto(), Dinero::sumar);
                    });
            return suma.entrySet().stream()
                    .map(e -> new TotalPorCategoria(CategoriaDTO.de(cats.get(e.getKey())), e.getValue().getCantidad(), null))
                    .toList();
        }
        public List<TotalDiario> sumarPorDia(UUID u, LocalDate desde, LocalDate hasta) {
            Map<String, TotalDiario> suma = new LinkedHashMap<>();
            datos.values().stream().filter(m -> m.perteneceA(u))
                    .filter(m -> !m.getFecha().isBefore(desde) && !m.getFecha().isAfter(hasta))
                    .forEach(m -> suma.merge(m.getFecha() + m.getTipo().name(),
                            new TotalDiario(m.getFecha(), m.getTipo(), m.getMonto().getCantidad()),
                            (a, b) -> new TotalDiario(a.fecha(), a.tipo(), a.total().add(b.total()))));
            return List.copyOf(suma.values());
        }
        public Dinero sumarGastos(UUID u, UUID cat, YearMonth periodo) {
            return datos.values().stream().filter(m -> m.perteneceA(u) && m instanceof Gasto)
                    .filter(m -> m.getCategoria().getId().equals(cat) && YearMonth.from(m.getFecha()).equals(periodo))
                    .map(Movimiento::getMonto).reduce(Dinero.cero(Dinero.COP), Dinero::sumar);
        }
        public long contarPorCategoria(UUID u, UUID cat) {
            return datos.values().stream().filter(m -> m.perteneceA(u) && m.getCategoria().getId().equals(cat)).count();
        }
        public int reasignarCategoria(UUID u, UUID origen, UUID destino) {
            Categoria nueva = categorias.datos.get(destino);
            int n = 0;
            for (Movimiento m : datos.values()) {
                if (m.perteneceA(u) && m.getCategoria().getId().equals(origen)) {
                    asignar(m, nueva);
                    n++;
                }
            }
            return n;
        }
        private static void asignar(Movimiento m, Categoria c) {
            try {
                Field f = Movimiento.class.getDeclaredField("categoria");
                f.setAccessible(true);
                f.set(m, c);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static class Presupuestos implements IRepositorioPresupuestos {
        public final Map<UUID, Presupuesto> datos = new LinkedHashMap<>();
        public void guardar(Presupuesto p) { datos.put(p.getId(), p); }
        public void eliminar(Presupuesto p) { datos.remove(p.getId()); }
        public Optional<Presupuesto> buscar(UUID u, UUID c, YearMonth per) {
            return datos.values().stream().filter(p -> p.getIdUsuario().equals(u) && p.getCategoria().getId().equals(c)
                    && p.getPeriodo().equals(per)).findFirst();
        }
        public Optional<Presupuesto> buscarPorId(UUID u, UUID id) {
            return Optional.ofNullable(datos.get(id)).filter(p -> p.getIdUsuario().equals(u));
        }
        public List<Presupuesto> buscarPorPeriodo(UUID u, YearMonth per) {
            return datos.values().stream().filter(p -> p.getIdUsuario().equals(u) && p.getPeriodo().equals(per)).toList();
        }
        public void eliminarPorCategoria(UUID u, UUID c) {
            datos.values().removeIf(p -> p.getIdUsuario().equals(u) && p.getCategoria().getId().equals(c));
        }
    }

    /** Reversible "hash" so tests stay fast (BCrypt is covered by the integration test). */
    public static class Cifrador implements ICifradorContrasena {
        public String cifrar(String clave) { return "h:" + clave; }
        public boolean coincide(String clave, String hash) { return hash.equals("h:" + clave); }
    }

    public static class Emisor implements IEmisorTokens {
        public final Map<String, UUID> sesiones = new HashMap<>();
        public final Map<String, UUID> recuperaciones = new HashMap<>();
        public final Set<String> usados = new HashSet<>();
        private final Set<String> retirados = new HashSet<>();
        public TokensDTO emitir(Usuario u) {
            String r = "r-" + UUID.randomUUID();
            sesiones.put(r, u.getId());
            return new TokensDTO("a-" + u.getId(), r, Instant.EPOCH);
        }
        public UUID validarAcceso(String t) { return UUID.fromString(t.substring(2)); }
        public UUID validarRefresh(String t) {
            UUID id = sesiones.get(t);
            if (id == null) throw new ReglaNegocioException("TOKEN_INVALIDO", "x");
            return id;
        }
        public void revocar(String t) { sesiones.remove(t); }
        public void retirar(String t) {
            if (!retirados.add(t)) sesiones.remove(t);   // same rule as EmisorTokensJwt, without the clock
        }
        public void revocarTodas(UUID id) { sesiones.values().removeIf(id::equals); }
        public String crearTokenRecuperacion(UUID id) {
            String t = "rec-" + UUID.randomUUID();
            recuperaciones.put(t, id);
            return t;
        }
        public UUID usarTokenRecuperacion(String t) {
            if (!recuperaciones.containsKey(t) || !usados.add(t)) throw new ReglaNegocioException("ENLACE_INVALIDO", "x");
            return recuperaciones.get(t);
        }
    }

    public static class Notificador implements INotificador {
        public final List<String> enlaces = new ArrayList<>();
        public final List<PresupuestoDTO> alertas = new ArrayList<>();
        public boolean disponible = true;
        @Override public boolean puedeEnviar() { return disponible; }
        public void enviarRecuperacion(Email c, String enlace) { enlaces.add(enlace); }
        public void enviarAlertaPresupuesto(Email c, PresupuestoDTO p) { alertas.add(p); }
    }

    static Map<String, Long> contar(List<String> l) {
        return l.stream().collect(Collectors.groupingBy(s -> s, Collectors.counting()));
    }
}
