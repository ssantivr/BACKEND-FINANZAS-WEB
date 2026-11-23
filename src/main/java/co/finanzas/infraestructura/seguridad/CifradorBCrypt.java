package co.finanzas.infraestructura.seguridad;

import co.finanzas.aplicacion.puertos.salida.ICifradorContrasena;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** RN-07: BCrypt with cost 12. */
@Component
public class CifradorBCrypt implements ICifradorContrasena {

    private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder(12);

    @Override
    public String cifrar(String clave) {
        return codificador.encode(clave);
    }

    @Override
    public boolean coincide(String clave, String hash) {
        return codificador.matches(clave, hash);
    }
}
