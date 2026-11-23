package co.finanzas.infraestructura.seguridad;

import co.finanzas.aplicacion.puertos.salida.IEmisorTokens;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class ConfiguracionSeguridad {

    @Bean
    public SecurityFilterChain cadenaFiltros(HttpSecurity http, IEmisorTokens emisor) throws Exception {
        http.csrf(c -> c.disable())   // stateless API: the access token travels in a header, not in a cookie
                .cors(c -> { })
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/**", "/actuator/health", "/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401);
                    res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                    res.setCharacterEncoding("UTF-8");
                    res.getWriter().write("{\"type\":\"about:blank\",\"title\":\"No autenticado\",\"status\":401,"
                            + "\"detail\":\"Inicia sesión para continuar\",\"codigo\":\"NO_AUTENTICADO\"}");
                }))
                .addFilterBefore(new FiltroJwt(emisor), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource origenesPermitidos(@Value("${app.cors.origenes}") String origenes) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(Arrays.stream(origenes.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        c.setAllowCredentials(true);   // the refresh-token cookie
        c.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", c);
        return fuente;
    }
}
