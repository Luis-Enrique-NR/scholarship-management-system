package pe.com.security.scholarship.security.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import pe.com.security.scholarship.security.model.BaseUser;
import pe.com.security.scholarship.security.oauth2.CustomOAuth2UserService;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final CustomOAuth2UserService customOAuth2UserService;

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public SecurityFilterChain oAuth2SecurityFilterChain(HttpSecurity http) throws Exception {
    OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
            new OAuth2AuthorizationServerConfigurer();

    // Esta cadena SOLO se encarga de los endpoints de protocolo OAuth2 (.well-known, /oauth2/token, etc)
    http
            .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
            .with(authorizationServerConfigurer, (token) ->
                    token.oidc(Customizer.withDefaults())
            )
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(
                    new LoginUrlAuthenticationEntryPoint("/login")
            ))
            // Quitamos el formLogin de aquí porque esta cadena no maneja el /login visual
            .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));

    return http.build();
  }

  @Bean
  public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
    http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(Customizer.withDefaults())
            .oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
            )
            .authorizeHttpRequests(auth -> auth
                    // 1. El registro es PÚBLICO
                    .requestMatchers("/api/v1/auth/register/**").permitAll()
                    // 2. Swagger y recursos estáticos son PÚBLICOS
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/oauth2-redirect.html").permitAll()
                    // 3. El resto (como /password) requiere AUTH
                    .anyRequest().authenticated()
            )
            // Esto permite que el Auth Server también actúe como Resource Server de sí mismo
            // para endpoints como el cambio de contraseña que envían un Bearer Token
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

    return http.build();
  }

  // --- SEGURIDAD DE TOKENS (JWT) ---


  @Bean
  public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
    return context -> {
      // Usamos constantes de Spring en lugar de Strings planos para evitar errores de dedo
      if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
        customizeAccessToken(context);
      } else if (OAuth2TokenType.REFRESH_TOKEN.equals(context.getTokenType())) {
        customizeRefreshToken(context);
      }
    };
  }

  private void customizeAccessToken(JwtEncodingContext context) {
    Authentication authentication = context.getPrincipal();

    // Extraemos autoridades de forma limpia
    Set<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());

    context.getClaims().claims(claims -> {
      claims.put("roles", roles);
      claims.put("iss_at", LocalDateTime.now().toString()); // Timestamp de emisión real

      Object principal = authentication.getPrincipal();

      if (principal instanceof BaseUser user) {
        claims.put("uid", user.getUsuarioId());
        claims.put("name", user.getNombreCompleto());
      }
    });
  }

  private void customizeRefreshToken(JwtEncodingContext context) {
    context.getClaims().claims(claims -> {
      claims.put("jti", UUID.randomUUID().toString()); // Añadimos un ID único al refresh token para trazabilidad
      claims.put("ref_info", "Authorized by Security Core");
    });
  }

  // --- INFRAESTRUCTURA DE FIRMA ---

  @Bean
  public JWKSource<SecurityContext> jwkSource() {
    RSAKey rsaKey = JwksKeys.generateRSAKey();
    JWKSet jwkSet = new JWKSet(rsaKey);
    return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
  }

  @Bean
  public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
    return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
  }

  @Bean
  public AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder().build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}