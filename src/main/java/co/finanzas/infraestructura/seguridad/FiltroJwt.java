package co.finanzas.infraestructura.seguridad;

import co.finanzas.aplicacion.puertos.salida.IEmisorTokens;
import co.finanzas.dominio.excepciones.ReglaNegocioException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/** Reads "Authorization: Bearer ..." and puts the user id (UUID) as the authenticated principal. */
public class FiltroJwt extends OncePerRequestFilter {

    private final IEmisorTokens emisor;

    public FiltroJwt(IEmisorTokens emisor) {
        this.emisor = emisor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String cabecera = peticion.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            try {
                UUID idUsuario = emisor.validarAcceso(cabecera.substring(7));
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(idUsuario, null, List.of()));
            } catch (ReglaNegocioException e) {
                SecurityContextHolder.clearContext();
            }
        }
        cadena.doFilter(peticion, respuesta);
    }
}
