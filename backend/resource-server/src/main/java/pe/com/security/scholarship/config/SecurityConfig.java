package pe.com.security.scholarship.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
@Profile("!test")
public class SecurityConfig {

  private final CustomAccessDeniedHandler customAccessDeniedHandler;

  @Bean
  public SecurityFilterChain resourceServerFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
    http
            // Paso 1: Definimos qué rutas protege esta cadena
            // IMPORTANTE: Quitamos el matcher restrictivo para que esta cadena sea la "red de seguridad"
            // pero mantenemos la protección de las APIs.
            .authorizeHttpRequests(auth -> auth
                    // Rutas públicas de Swagger
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    // Rutas de negocio públicas
                    .requestMatchers("/api/v1/convocatorias/activa").permitAll()
                    // El resto requiere autenticación
                    .anyRequest().authenticated()
            )
            // Paso 2: Habilitamos el formulario de login (esto creará la ruta /login automáticamente)
            .formLogin(Customizer.withDefaults())
            .oauth2ResourceServer(oauth -> oauth
                    .jwt(jwt -> jwt
                            .decoder(jwtDecoder) // <--- USA EL BEAN INYECTADO
                            .jwtAuthenticationConverter(jwtAuthenticationConverter())
                    )
            )
            .exceptionHandling(ex -> ex
                    .accessDeniedHandler(customAccessDeniedHandler)
            )
            .csrf(AbstractHttpConfigurer::disable);

    return http.build();
  }

  @Bean
  public JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
    return JwtDecoders.fromIssuerLocation(issuerUri);
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