package pe.com.security.scholarship.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.security.scholarship.dto.request.RegisterConvocatoriaRequest;
import pe.com.security.scholarship.dto.response.AuditEmpleadoResponse;
import pe.com.security.scholarship.dto.response.RegisteredConvocatoriaResponse;
import pe.com.security.scholarship.entity.Convocatoria;
import pe.com.security.scholarship.entity.Empleado;
import pe.com.security.scholarship.entity.enums.EstadoConvocatoria;
import pe.com.security.scholarship.entity.enums.Mes;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.repository.ConvocatoriaRepository;
import pe.com.security.scholarship.repository.EmpleadoRepository;
import pe.com.security.scholarship.util.SecurityUtils;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConvocatoriaServiceTest {

    @Mock
    private ConvocatoriaRepository convocatoriaRepository;

    @Mock
    private EmpleadoRepository empleadoRepository;

    @Mock
    private EmpleadoService empleadoService;

    @InjectMocks
    private ConvocatoriaService convocatoriaService;

    @AfterEach
    void tearDown() {
        // No static mocks to close here as we use try-with-resources
    }

    @Test
    void crearConvocatoriaExitosamente() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        RegisterConvocatoriaRequest request = new RegisterConvocatoriaRequest();
        request.setMes(Mes.ENERO);
        request.setFechaInicio(LocalDate.now());
        request.setFechaFin(LocalDate.now().plusDays(30));
        request.setCantidadVacantes(10);

        Empleado empleado = new Empleado();
        empleado.setIdUsuario(idUsuario);

        AuditEmpleadoResponse auditResponse = AuditEmpleadoResponse.builder()
                .codigo("EMP-001")
                .nombreCompleto("Test User")
                .build();

        Convocatoria convocatoriaGuardada = Convocatoria.builder()
                .id(1)
                .mes(request.getMes())
                .fechaInicio(request.getFechaInicio())
                .fechaFin(request.getFechaFin())
                .estado(EstadoConvocatoria.PROGRAMADO)
                .cantidadVacantes(request.getCantidadVacantes())
                .createdBy(empleado)
                .build();

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);
            
            when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(empleado));
            when(convocatoriaRepository.save(any(Convocatoria.class))).thenReturn(convocatoriaGuardada);
            when(empleadoService.obtenerAuditoriaActual()).thenReturn(auditResponse);

            // Act
            RegisteredConvocatoriaResponse response = convocatoriaService.registerConvocatoria(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getMes()).isEqualTo(request.getMes());
            assertThat(response.getCantidadVacantes()).isEqualTo(request.getCantidadVacantes());
            assertThat(response.getCreatedBy()).isEqualTo(auditResponse);
            
            verify(empleadoRepository, times(1)).findByIdUsuario(idUsuario);
            verify(convocatoriaRepository, times(1)).save(any(Convocatoria.class));
            verify(empleadoService, times(1)).obtenerAuditoriaActual();
        }
    }

    @Test
    void lanzarExcepcionCuandoEmpleadoNoExisteAlCrear() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        RegisterConvocatoriaRequest request = new RegisterConvocatoriaRequest();

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);
            
            when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> convocatoriaService.registerConvocatoria(request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("No se encontró empleado con el user id del payload");
            
            verify(empleadoRepository, times(1)).findByIdUsuario(idUsuario);
            verify(convocatoriaRepository, times(0)).save(any(Convocatoria.class));
        }
    }
}
