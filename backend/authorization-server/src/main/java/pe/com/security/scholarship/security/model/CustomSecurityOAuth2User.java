package pe.com.security.scholarship.security.model;

import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import pe.com.security.scholarship.entity.Usuario;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
public class CustomSecurityOAuth2User implements OAuth2User, BaseUser {

  private final Usuario usuario; // Tu entidad de BD
  private final Map<String, Object> attributes; // Los datos que vienen de Google

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return usuario.getRoles().stream()
            .map(rol -> new SimpleGrantedAuthority(rol.getNombre()))
            .toList();
  }

  @Override
  public String getName() {
    return usuario.getCorreo();
  }

  @Override
  public UUID getUsuarioId() { return usuario.getId(); }

  @Override
  public String getNombreCompleto() {
    return usuario.getNombres() + " " + usuario.getApellidos();
  }
}