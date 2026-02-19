package pe.com.security.scholarship.security.oauth2;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.security.scholarship.mapper.UsuarioMapper;
import pe.com.security.scholarship.security.model.CustomSecurityOAuth2User;
import pe.com.security.scholarship.entity.Rol;
import pe.com.security.scholarship.entity.Usuario;
import pe.com.security.scholarship.entity.enums.AuthProvider;
import pe.com.security.scholarship.repository.RolRepository;
import pe.com.security.scholarship.repository.UsuarioRepository;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  private final UsuarioRepository usuarioRepository;
  private final RolRepository rolRepository;

  @Override
  @Transactional // Recomendado al realizar operaciones de escritura
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    // 1. Obtenemos los datos de Google/GitHub
    OAuth2User oAuth2User = super.loadUser(userRequest);

    // 2. Extraemos la información
    String email = oAuth2User.getAttribute("email");
    String googleId = oAuth2User.getAttribute("sub");
    String name = oAuth2User.getAttribute("given_name");
    String lastName = oAuth2User.getAttribute("family_name");

    // 3. Lógica de registro o mapeo
    Usuario usuario = usuarioRepository.findByCorreoWithRoles(email)
            .map(existingUser -> {
              // Si el usuario existía pero no tenía providerId (ej. se registró manual), lo vinculamos
              if (existingUser.getProviderId() == null) {
                existingUser.setProviderId(oAuth2User.getAttribute("sub"));
                existingUser.setProvider(AuthProvider.GOOGLE);
                return usuarioRepository.save(existingUser);
              }
              return existingUser;
            })
            .orElseGet(() -> {
              // Si no existe, buscamos el rol y lo creamos
              Rol defaultRol = rolRepository.findByNombre("ROLE_STUDENT")
                      .orElseThrow(() -> new RuntimeException("Error: Rol ROLE_STUDENT no encontrado en la base de datos"));

              return usuarioRepository.save(UsuarioMapper.buildUsuarioGoogle(email, name, lastName, googleId, defaultRol));
            });

    return new CustomSecurityOAuth2User(usuario, oAuth2User.getAttributes());
  }
}