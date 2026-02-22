package pe.com.security.scholarship.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@TestConfiguration
@Profile("test")
@EnableWebSecurity
@EnableMethodSecurity
public class TestSecurityConfig {

  @Bean
  public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
    http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                    // Mantener las rutas públicas que definiste en tu config real
                    .requestMatchers("/api/v1/convocatorias/activa").permitAll()
                    // El resto DEBE estar autenticado para probar @PreAuthorize
                    .anyRequest().authenticated()
            )
            // IMPORTANTE: Necesitamos el converter para que Spring entienda los roles del JWT mockeado
            .oauth2ResourceServer(oauth -> oauth
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );
    return http.build();
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
    // Cambiamos "" por "ROLE_" para que hasRole('XYZ') funcione
    grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
    return jwtAuthenticationConverter;
  }

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper()
            .registerModule(new JavaTimeModule()) // Útil si usas fechas
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @Bean
  @Primary // Esto asegura que Spring use este bean y no el real durante los tests
  public JwtDecoder jwtDecoder() {
    // Creamos un decoder que no hace peticiones HTTP
    return token -> {
      try {
        // Puedes personalizar los claims según lo que tus Services necesiten (uid, roles, etc.)
        return Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject("test-user")
                .claim("uid", "aa23a95d-9af4-4a5c-94d8-072a2845c933")
                .claim("name", "Test User")
                .claim("roles", List.of("ROLE_ADMIN"))
                .build();
      } catch (Exception e) {
        throw new JwtException("Error en el mock del decoder", e);
      }
    };
  }
}