package pe.com.security.scholarship.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import pe.com.security.scholarship.domain.entity.Empleado;
import pe.com.security.scholarship.domain.entity.Estudiante;
import pe.com.security.scholarship.domain.entity.PeriodoAcademico;
import pe.com.security.scholarship.domain.entity.PromedioPonderado;
import pe.com.security.scholarship.dto.ProcesamientoResult;
import pe.com.security.scholarship.dto.request.PromedioCsvRequest;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.repository.EmpleadoRepository;
import pe.com.security.scholarship.repository.EstudianteRepository;
import pe.com.security.scholarship.repository.PeriodoAcademicoRepository;
import pe.com.security.scholarship.repository.PromedioPonderadoRepository;
import pe.com.security.scholarship.util.CargaMasivaHelper;
import pe.com.security.scholarship.util.SecurityUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromedioPonderadoServiceTest {

    @Mock
    private CargaMasivaHelper cargaMasivaHelper;
    @Mock
    private EstudianteRepository estudianteRepository;
    @Mock
    private EmpleadoRepository empleadoRepository;
    @Mock
    private PromedioPonderadoRepository promedioRepository;
    @Mock
    private PeriodoAcademicoRepository periodoAcademicoRepository;

    @InjectMocks
    private PromedioPonderadoService promedioPonderadoService;

    @AfterEach
    void tearDown() {
        // No static mocks to close here as we use try-with-resources
    }

    @Test
    void testProcesarCargaPromedios_Exito() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        String periodo = "2023-1";
        MultipartFile file = mock(MultipartFile.class);
        
        Empleado empleado = new Empleado();
        empleado.setId(UUID.randomUUID());
        
        PeriodoAcademico periodoAcademico = new PeriodoAcademico();
        periodoAcademico.setPeriodo(periodo);
        
        Estudiante estudiante = new Estudiante();
        estudiante.setCodigoEstudiante("20201001");

        PromedioCsvRequest fila = new PromedioCsvRequest();
        fila.setCodigo("20201001");
        fila.setCiclo(5);
        fila.setPromedio(15.5);

        ProcesamientoResult expectedResult = ProcesamientoResult.builder().total(1).exitos(1).build();

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);
            
            when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(empleado));
            when(periodoAcademicoRepository.findByPeriodo(periodo)).thenReturn(Optional.of(periodoAcademico));
            
            // Simulamos que el helper ejecuta el consumer
            when(cargaMasivaHelper.procesar(eq(file), eq(PromedioCsvRequest.class), any()))
                    .thenAnswer(invocation -> {
                        Consumer<PromedioCsvRequest> consumer = invocation.getArgument(2);
                        // Configuramos el mock del estudiante para cuando se ejecute el consumer
                        when(estudianteRepository.findByCodigo("20201001")).thenReturn(Optional.of(estudiante));
                        consumer.accept(fila);
                        return expectedResult;
                    });

            // Act
            ProcesamientoResult result = promedioPonderadoService.procesarCargaPromedios(file, periodo);

            // Assert
            assertThat(result).isEqualTo(expectedResult);
            
            ArgumentCaptor<PromedioPonderado> captor = ArgumentCaptor.forClass(PromedioPonderado.class);
            verify(promedioRepository).save(captor.capture());
            
            PromedioPonderado saved = captor.getValue();
            assertThat(saved.getEstudiante()).isEqualTo(estudiante);
            assertThat(saved.getPeriodoAcademico()).isEqualTo(periodoAcademico);
            assertThat(saved.getEmpleado()).isEqualTo(empleado);
            assertThat(saved.getCicloRelativo()).isEqualTo(5);
            assertThat(saved.getPromedioPonderado()).isEqualTo(15.5);
        }
    }

    @Test
    void testProcesarCargaPromedios_PeriodoNoEncontrado() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        String periodo = "2023-1";
        MultipartFile file = mock(MultipartFile.class);
        Empleado empleado = new Empleado();

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);
            
            when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(empleado));
            when(periodoAcademicoRepository.findByPeriodo(periodo)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> promedioPonderadoService.procesarCargaPromedios(file, periodo))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Periodo académico no registrado");
        }
    }

    @Test
    void testLogicaInterna_PromedioInvalido() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        String periodo = "2023-1";
        MultipartFile file = mock(MultipartFile.class);
        Empleado empleado = new Empleado();
        PeriodoAcademico periodoAcademico = new PeriodoAcademico();
        Estudiante estudiante = new Estudiante();

        PromedioCsvRequest fila = new PromedioCsvRequest();
        fila.setCodigo("20201001");
        fila.setPromedio(25.0); // Promedio inválido

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);
            when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(empleado));
            when(periodoAcademicoRepository.findByPeriodo(periodo)).thenReturn(Optional.of(periodoAcademico));

            when(cargaMasivaHelper.procesar(eq(file), eq(PromedioCsvRequest.class), any()))
                    .thenAnswer(invocation -> {
                        Consumer<PromedioCsvRequest> consumer = invocation.getArgument(2);
                        when(estudianteRepository.findByCodigo("20201001")).thenReturn(Optional.of(estudiante));
                        
                        // Ejecutamos y verificamos la excepción
                        assertThatThrownBy(() -> consumer.accept(fila))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessage("El promedio debe estar entre 0 y 20");
                        
                        return null;
                    });

            // Act
            promedioPonderadoService.procesarCargaPromedios(file, periodo);
        }
    }

    @Test
    void testLogicaInterna_CicloInvalido() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        String periodo = "2023-1";
        MultipartFile file = mock(MultipartFile.class);
        Empleado empleado = new Empleado();
        PeriodoAcademico periodoAcademico = new PeriodoAcademico();
        Estudiante estudiante = new Estudiante();

        PromedioCsvRequest fila = new PromedioCsvRequest();
        fila.setCodigo("20201001");
        fila.setPromedio(15.0);
        fila.setCiclo(15); // Ciclo inválido

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);
            when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(empleado));
            when(periodoAcademicoRepository.findByPeriodo(periodo)).thenReturn(Optional.of(periodoAcademico));

            when(cargaMasivaHelper.procesar(eq(file), eq(PromedioCsvRequest.class), any()))
                    .thenAnswer(invocation -> {
                        Consumer<PromedioCsvRequest> consumer = invocation.getArgument(2);
                        when(estudianteRepository.findByCodigo("20201001")).thenReturn(Optional.of(estudiante));
                        
                        // Ejecutamos y verificamos la excepción
                        assertThatThrownBy(() -> consumer.accept(fila))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessage("Ciclo académico inválido");
                        
                        return null;
                    });

            // Act
            promedioPonderadoService.procesarCargaPromedios(file, periodo);
        }
    }

    @Test
    void testLogicaInterna_EstudianteNoEncontrado() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        String periodo = "2023-1";
        MultipartFile file = mock(MultipartFile.class);
        Empleado empleado = new Empleado();
        PeriodoAcademico periodoAcademico = new PeriodoAcademico();

        PromedioCsvRequest fila = new PromedioCsvRequest();
        fila.setCodigo("NO_EXISTE");

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);
            when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(empleado));
            when(periodoAcademicoRepository.findByPeriodo(periodo)).thenReturn(Optional.of(periodoAcademico));

            when(cargaMasivaHelper.procesar(eq(file), eq(PromedioCsvRequest.class), any()))
                    .thenAnswer(invocation -> {
                        Consumer<PromedioCsvRequest> consumer = invocation.getArgument(2);
                        
                        when(estudianteRepository.findByCodigo("NO_EXISTE")).thenReturn(Optional.empty());
                        
                        // Ejecutamos y verificamos la excepción
                        assertThatThrownBy(() -> consumer.accept(fila))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessage("Estudiante no encontrado");
                        
                        return null;
                    });

            // Act
            promedioPonderadoService.procesarCargaPromedios(file, periodo);
        }
    }
}
