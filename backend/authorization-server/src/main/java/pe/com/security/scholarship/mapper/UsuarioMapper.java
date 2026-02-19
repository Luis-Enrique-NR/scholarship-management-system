package pe.com.security.scholarship.mapper;

import pe.com.security.scholarship.dto.request.RegisterUserRequest;
import pe.com.security.scholarship.dto.response.RegisteredUserResponse;
import pe.com.security.scholarship.entity.Rol;
import pe.com.security.scholarship.entity.Usuario;
import pe.com.security.scholarship.entity.enums.AuthProvider;

import java.time.Instant;
import java.util.Collections;

public class UsuarioMapper {

  public static Usuario buildUsuarioLocal(RegisterUserRequest dto, String hashPsw, Rol defaultRol) {
    return Usuario.builder()
            .nombres(dto.getNombres())
            .apellidos(dto.getApellidos())
            .correo(dto.getCorreo())
            .password(hashPsw)
            .habilitado(Boolean.TRUE)
            .createdAt(Instant.now())
            .provider(AuthProvider.LOCAL)
            .build();
  }

  public static Usuario buildUsuarioGoogle(String email, String name, String lastName, String googleId, Rol defaultRol) {
    return Usuario.builder()
            .correo(email)
            .nombres(name)
            .apellidos(lastName)
            .provider(AuthProvider.GOOGLE)
            .providerId(googleId)
            .habilitado(Boolean.TRUE)
            .createdAt(Instant.now())
            .roles(Collections.singletonList(defaultRol))
            .build();
  }

  public static RegisteredUserResponse mapRegisteredUsuario(Usuario usuario) {
    return RegisteredUserResponse.builder()
            .nombres(usuario.getNombres())
            .apellidos(usuario.getApellidos())
            .correo(usuario.getCorreo())
            .habilitado(usuario.getHabilitado())
            .build();
  }
}
