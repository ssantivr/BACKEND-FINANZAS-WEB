package co.finanzas.aplicacion.puertos.salida;

public interface ICifradorContrasena {
    String cifrar(String clave);
    boolean coincide(String clave, String hash);
}
