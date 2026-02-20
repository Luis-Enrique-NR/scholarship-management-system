package pe.com.security.scholarship.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import pe.com.security.scholarship.dto.response.AuditEmpleadoResponse;
import pe.com.security.scholarship.entity.Empleado;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.repository.EmpleadoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmpleadoServiceTest {

    @Mock
    private EmpleadoRepository empleadoRepository;

    @InjectMocks
    private EmpleadoService empleadoService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnAuditResponseWhenUserIsAuthenticated() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        String codigoEmpleado = "EMP-001";
        String nombreCompleto = "Juan Perez";
        List<String> roles = List.of("ROLE_USER");

        // Mock JWT
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("uid")).thenReturn(idUsuario.toString());
        when(jwt.getClaimAsString("name")).thenReturn(nombreCompleto);
        when(jwt.getClaimAsStringList("roles")).thenReturn(roles);

        // Mock Security Context
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(jwt);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Mock Repository
        Empleado empleado = Empleado.builder()
                .idUsuario(idUsuario)
                .codigoEmpleado(codigoEmpleado)
                .build();

        when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(empleado));

        // Act
        AuditEmpleadoResponse response = empleadoService.obtenerAuditoriaActual();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getCodigo()).isEqualTo(codigoEmpleado);
        assertThat(response.getNombreCompleto()).isEqualTo(nombreCompleto);
        assertThat(response.getRol()).isEqualTo(roles);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenEmpleadoDoesNotExist() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();

        // Mock JWT
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("uid")).thenReturn(idUsuario.toString());

        // Mock Security Context
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(jwt);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Mock Repository returning empty
        when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> empleadoService.obtenerAuditoriaActual())
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Empleado no vinculado al usuario");
    }

    @Test
    void shouldThrowRuntimeExceptionWhenNotAuthenticated() {
        // Arrange
        // Mock Authentication where principal is NOT a Jwt (e.g. a String)
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        assertThatThrownBy(() -> empleadoService.obtenerAuditoriaActual())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuario no autenticado con JWT");
    }
}
