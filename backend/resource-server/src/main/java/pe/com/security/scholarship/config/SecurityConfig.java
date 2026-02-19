package pe.com.security.scholarship.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final CustomAccessDeniedHandler customAccessDeniedHandler;

  @Bean
  public SecurityFilterChain resourceServerFilterChain(HttpSecurity http) throws Exception {
    http
            // Paso 1: Definimos qué rutas protege esta cadena
            // IMPORTANTE: Quitamos el matcher restrictivo para que esta cadena sea la "red de seguridad"
            // pero mantenemos la protección de las APIs.
            .authorizeHttpRequests(auth -> auth
                    // Rutas públicas de Swagger
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    // Rutas de negocio públicas
                    .requestMatchers("/api/v1/auth/register").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/usuario/convocatorias").permitAll()
                    // El resto requiere autenticación
                    .anyRequest().authenticated()
            )
            // Paso 2: Habilitamos el formulario de login (esto creará la ruta /login automáticamente)
            .formLogin(Customizer.withDefaults())

            // Paso 3: Configuración del Resource Server para los tokens JWT
            .oauth2ResourceServer(oauth -> oauth
                    .jwt(configJwt -> configJwt
                            .decoder(JwtDecoders.fromIssuerLocation("http://localhost:9000"))))
            .exceptionHandling(ex -> ex
                    .accessDeniedHandler(customAccessDeniedHandler)
            )
            .csrf(AbstractHttpConfigurer::disable);

    return http.build();
  }

  // --- SEGURIDAD DE TOKENS (JWT) ---

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
    grantedAuthoritiesConverter.setAuthorityPrefix("");

    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
    return jwtAuthenticationConverter;
  }

}