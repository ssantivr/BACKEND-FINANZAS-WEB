package co.finanzas.infraestructura.persistencia;

import co.finanzas.aplicacion.puertos.salida.IRepositorioUsuarios;
import co.finanzas.dominio.excepciones.ReglaNegocioException;
import co.finanzas.dominio.modelo.Usuario;
import co.finanzas.dominio.tipos.EstadoCuenta;
import co.finanzas.dominio.valor.Email;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class RepositorioUsuariosAdaptador implements IRepositorioUsuarios {

    private final UsuarioRepositorioMongo mongo;

    public RepositorioUsuariosAdaptador(UsuarioRepositorioMongo mongo) {
        this.mongo = mongo;
    }

    @Override
    public void guardar(Usuario u) {
        UsuarioDocumento d = new UsuarioDocumento();
        d.id = u.getId().toString();
        d.correo = u.getCorreo().getValor();
        d.nombre = u.getNombre();
        d.hashContrasena = u.getHashContrasena();
        d.estado = u.getEstado().name();
        d.intentosFallidos = u.getIntentosFallidos();
        d.bloqueadoHasta = u.getBloqueadoHasta();
        d.fechaRegistro = u.getFechaRegistro();
        d.eliminadaEn = u.getEliminadaEn();
        try {
            mongo.save(d);
        } catch (DuplicateKeyException e) {
            throw new ReglaNegocioException("CORREO_DUPLICADO", "Este correo ya está registrado");
        }
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return mongo.findById(id.toString()).map(RepositorioUsuariosAdaptador::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(Email correo) {
        return mongo.findByCorreo(correo.getValor()).map(RepositorioUsuariosAdaptador::aDominio);
    }

    @Override
    public boolean existeCorreo(Email correo) {
        return mongo.existsByCorreo(correo.getValor());
    }

    private static Usuario aDominio(UsuarioDocumento d) {
        return new Usuario(UUID.fromString(d.id), d.nombre, new Email(d.correo), d.hashContrasena,
                EstadoCuenta.valueOf(d.estado), d.intentosFallidos, d.bloqueadoHasta, d.fechaRegistro, d.eliminadaEn);
    }
}
