package pe.com.security.scholarship.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.security.scholarship.mapper.UsuarioMapper;
import pe.com.security.scholarship.dto.request.RegisterUserRequest;
import pe.com.security.scholarship.dto.request.UpdatePasswordRequest;
import pe.com.security.scholarship.dto.response.RegisteredUserResponse;
import pe.com.security.scholarship.entity.Rol;
import pe.com.security.scholarship.entity.Usuario;
import pe.com.security.scholarship.exception.BadCredentialsException;
import pe.com.security.scholarship.exception.ConflictException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.repository.RolRepository;
import pe.com.security.scholarship.repository.UsuarioRepository;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

  private final UsuarioRepository usuarioRepository;
  private final RolRepository rolRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public RegisteredUserResponse registerStudent(RegisterUserRequest request) {
    // 1. Validación de existencia
    if (usuarioRepository.existsByCorreo(request.getCorreo())) {
      throw new ConflictException("El correo ya está registrado");
    }

    // 2. Mapeo y Encriptación (Uso de PasswordEncoder inyectado)
    String hashPsw = passwordEncoder.encode(request.getPassword());

    // 3. Asignación de Rol por defecto (Empresarialmente se busca en BD)
    Rol rol = rolRepository.findByNombre("ROLE_STUDENT")
            .orElseThrow(() -> new NotFoundException("Error: Rol ROLE_STUDENT no encontrado en la base de datos"));

    Usuario user = UsuarioMapper.buildUsuarioLocal(request, hashPsw, rol);

    usuarioRepository.save(user);

    // 4. Respuesta Limpia: No devolvemos tokens aquí.
    // El frontend deberá redirigir al login de OAuth2 tras el registro.
    return UsuarioMapper.mapRegisteredUsuario(user);
  }

  @Transactional
  public void updatePassword(UpdatePasswordRequest request) {
    // 1. Obtener el Principal actual de forma segura
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String correo = auth.getName();

    Usuario usuario = usuarioRepository.findByCorreo(correo)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

    // 2. Validar contraseña actual
    if (!passwordEncoder.matches(request.getOldPassword(), usuario.getPassword())) {
      throw new BadCredentialsException("La contraseña actual es incorrecta");
    }

    // 3. Actualizar
    usuario.setPassword(passwordEncoder.encode(request.getNewPassword()));
    usuario.setUpdatedAt(Instant.now());
    usuarioRepository.save(usuario);
  }
}
