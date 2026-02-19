package pe.com.security.scholarship.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.security.scholarship.security.model.SecurityUser;
import pe.com.security.scholarship.repository.UsuarioRepository;

@Service("userDetailsService")
@RequiredArgsConstructor
public class JpaUserDetailsService implements UserDetailsService {

  private final UsuarioRepository usuarioRepository;

  @Override
  @Transactional(readOnly = true) // Importante: readOnly mejora el rendimiento
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return usuarioRepository.findByCorreoWithRoles(username)
            .map(SecurityUser::new) // Envolvemos la entidad en nuestro wrapper
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
  }
}
