package pe.com.security.scholarship.security.model;

import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pe.com.security.scholarship.entity.Usuario;

import java.util.Collection;
import java.util.UUID;

@AllArgsConstructor
public class SecurityUser implements UserDetails, BaseUser {

  private final Usuario usuario;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return usuario.getRoles().stream()
            .map(rol -> new SimpleGrantedAuthority(rol.getNombre()))
            .toList();
  }

  @Override
  public String getPassword() { return usuario.getPassword(); }

  @Override
  public String getUsername() { return usuario.getCorreo(); }

  @Override
  public boolean isEnabled() { return usuario.getHabilitado(); }

  @Override
  public UUID getUsuarioId() { return usuario.getId(); }

  @Override
  public String getNombreCompleto() {
    return usuario.getNombres() + " " + usuario.getApellidos();
  }

  @Override
  public boolean isAccountNonExpired() { return true; }
  @Override
  public boolean isAccountNonLocked() { return true; }
  @Override
  public boolean isCredentialsNonExpired() { return true; }
}