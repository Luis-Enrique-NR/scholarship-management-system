package pe.com.security.scholarship.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import pe.com.security.scholarship.dto.response.AuditEmpleadoResponse;
import pe.com.security.scholarship.domain.entity.Empleado;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.repository.EmpleadoRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmpleadoService {

  private final EmpleadoRepository empleadoRepository;

  public AuditEmpleadoResponse obtenerAuditoriaActual() {
    // 1. Obtener el JWT desde el contexto de seguridad
    var auth = SecurityContextHolder.getContext().getAuthentication();
    assert auth != null;
    if (!(auth.getPrincipal() instanceof Jwt jwt)) {
      throw new RuntimeException("Usuario no autenticado con JWT");
    }

    // 2. Extraer el UID para buscar al empleado en tu DB local
    UUID idUsuario = UUID.fromString(jwt.getClaimAsString("uid"));

    // 3. Buscar el código en tu tabla de empleados
    Empleado empleado = empleadoRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NotFoundException("Empleado no vinculado al usuario"));

    // 4. Construir el DTO mezclando ambas fuentes
    return AuditEmpleadoResponse.builder()
            .codigo(empleado.getCodigoEmpleado())
            .nombreCompleto(jwt.getClaimAsString("name"))
            .rol(jwt.getClaimAsStringList("roles"))
            .build();
  }
}
