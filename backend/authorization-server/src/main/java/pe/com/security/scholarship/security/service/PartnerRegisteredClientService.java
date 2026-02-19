package pe.com.security.scholarship.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import pe.com.security.scholarship.entity.Partner;
import pe.com.security.scholarship.exception.BadCredentialsException;
import pe.com.security.scholarship.repository.PartnerRepository;

import java.time.Duration;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class PartnerRegisteredClientService implements RegisteredClientRepository {

  private final PartnerRepository partnerRepository;

  @Override
  public void save(RegisteredClient registeredClient) {
    // En una implementación PRO, aquí podrías persistir nuevos clientes dinámicamente
    throw new UnsupportedOperationException("La persistencia de clientes no está habilitada vía API");
  }

  @Override
  @Cacheable(value = "partners_id", key = "#id")
  public RegisteredClient findById(String id) {
    return null;
    /*
    // Spring a veces busca por el ID interno (UUID), no por el ClientID
        return partnerRepository.findById(UUID.fromString(id))
                .map(this::mapToRegisteredClient)
                .orElse(null); // Spring espera null si no lo encuentra, no una excepción
     */
  }

  @Override
  @Cacheable(value = "partners_client_id", key = "#clientId")
  public RegisteredClient findByClientId(String clientId) {
    return partnerRepository.findByIdClient(clientId)
            .map(this::mapToRegisteredClient)
            .orElseThrow(() -> new BadCredentialsException("Cliente no identificado: " + clientId));
  }

  private RegisteredClient mapToRegisteredClient(Partner partner) {
    return RegisteredClient
            .withId(partner.getId().toString())
            .clientId(partner.getIdClient())
            .clientSecret(partner.getSecretClient())
            .clientName(partner.getNameClient())
            .redirectUris(uris ->
                    Arrays.stream(partner.getRedirectUri().split(","))
                            .map(String::trim)
                            .forEach(uris::add))
            .postLogoutRedirectUri(partner.getRedirectUriLogout())
            .clientAuthenticationMethods(methods ->
                    Arrays.stream(partner.getAuthenticationMethods().split(","))
                            .map(String::trim)
                            .map(ClientAuthenticationMethod::new)
                            .forEach(methods::add))
            .authorizationGrantTypes(grants ->
                    Arrays.stream(partner.getGrantTypes().split(","))
                            .map(String::trim)
                            .map(AuthorizationGrantType::new)
                            .forEach(grants::add))
            .scopes(scopes ->
                    Arrays.stream(partner.getScopes().split(","))
                            .map(String::trim)
                            .forEach(scopes::add))
            .tokenSettings(defaultTokenSettings())
            .clientSettings(ClientSettings.builder()
                    .requireProofKey(false)
                    .requireAuthorizationConsent(true)
                    .build())
            .build();
  }

  private TokenSettings defaultTokenSettings() {
    return TokenSettings.builder()
            .accessTokenTimeToLive(Duration.ofHours(2)) // 8 horas es mucho para producción
            .refreshTokenTimeToLive(Duration.ofDays(1)) // Añadimos Refresh Token
            .reuseRefreshTokens(false) // Mejor práctica de seguridad
            .build();
  }
}