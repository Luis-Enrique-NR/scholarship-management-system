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
import pe.com.security.scholarship.domain.entity.EvaluacionSocioeconomica;
import pe.com.security.scholarship.domain.enums.NivelSocioeconomico;
import pe.com.security.scholarship.dto.ProcesamientoResult;
import pe.com.security.scholarship.dto.request.EvaluacionCsvRequest;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.repository.EmpleadoRepository;
import pe.com.security.scholarship.repository.EstudianteRepository;
import pe.com.security.scholarship.repository.EvaluacionSocioeconomicaRepository;
import pe.com.security.scholarship.util.CargaMasivaHelper;
import pe.com.security.scholarship.util.SecurityUtils;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluacionSocioeconomicaServiceTest {

    @Mock
    private CargaMasivaHelper cargaMasivaHelper;
    @Mock
    private EstudianteRepository estudianteRepository;
    @Mock
    private EmpleadoRepository empleadoRepository;
    @Mock
    private EvaluacionSocioeconomicaRepository evaluacionRepository;

    @InjectMocks
    private EvaluacionSocioeconomicaService evaluacionService;

    @AfterEach
    void tearDown() {
        // No static mocks to close here as we use try-with-resources
    }

    @Test
    void testProcesarCargaMasiva_Exito() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        MultipartFile file = mock(MultipartFile.class);
        Empleado empleado = new Empleado();
        empleado.setId(UUID.randomUUID());

        Estudiante estudiante = new Estudiante();
        estudiante.setCodigoEstudiante("20201001");

        EvaluacionCsvRequest fila = new EvaluacionCsvRequest();
        fila.setCodigo("20201001");
        fila.setNivel("BUENO");
        fila.setFechaEvaluacion(LocalDate.now());

        ProcesamientoResult expectedResult = ProcesamientoResult.builder().total(1).exitos(1).build();

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);
            
            when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(empleado));
            
            // Simulamos que el helper ejecuta el consumer
            when(cargaMasivaHelper.procesar(eq(file), eq(EvaluacionCsvRequest.class), any()))
                    .thenAnswer(invocation -> {
                        Consumer<EvaluacionCsvRequest> consumer = invocation.getArgument(2);
                        // Configuramos el mock del estudiante para cuando se ejecute el consumer
                        when(estudianteRepository.findByCodigo("20201001")).thenReturn(Optional.of(estudiante));
                        consumer.accept(fila);
                        return expectedResult;
                    });

            // Act
            ProcesamientoResult result = evaluacionService.procesarCargaMasiva(file);

            // Assert
            assertThat(result).isEqualTo(expectedResult);
            
            ArgumentCaptor<EvaluacionSocioeconomica> captor = ArgumentCaptor.forClass(EvaluacionSocioeconomica.class);
            verify(evaluacionRepository).save(captor.capture());
            
            EvaluacionSocioeconomica saved = captor.getValue();
            assertThat(saved.getEstudiante()).isEqualTo(estudiante);
            assertThat(saved.getNivelSocioeconomico()).isEqualTo(NivelSocioeconomico.BUENO);
            assertThat(saved.getCreatedBy()).isEqualTo(empleado);
        }
    }

    @Test
    void testProcesarCargaMasiva_EmpleadoNoEncontrado() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        MultipartFile file = mock(MultipartFile.class);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);
            
            when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> evaluacionService.procesarCargaMasiva(file))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Empleado no encontrado");
            
            verify(cargaMasivaHelper, times(0)).procesar(any(), any(), any());
        }
    }

    @Test
    void testLogicaInterna_EstudianteNoEncontrado() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        MultipartFile file = mock(MultipartFile.class);
        Empleado empleado = new Empleado();

        EvaluacionCsvRequest fila = new EvaluacionCsvRequest();
        fila.setCodigo("NO_EXISTE");

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);
            when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(empleado));

            // Capturamos el consumer para ejecutarlo manualmente y probar la lógica interna
            when(cargaMasivaHelper.procesar(eq(file), eq(EvaluacionCsvRequest.class), any()))
                    .thenAnswer(invocation -> {
                        Consumer<EvaluacionCsvRequest> consumer = invocation.getArgument(2);
                        
                        // Configuramos el mock para que falle al buscar estudiante
                        when(estudianteRepository.findByCodigo("NO_EXISTE")).thenReturn(Optional.empty());
                        
                        // Ejecutamos y verificamos la excepción
                        assertThatThrownBy(() -> consumer.accept(fila))
                                .isInstanceOf(NotFoundException.class)
                                .hasMessage("Estudiante no encontrado");
                        
                        return null; // Retorno irrelevante para este test
                    });

            // Act
            evaluacionService.procesarCargaMasiva(file);
        }
    }

    @Test
    void testLogicaInterna_NivelInvalido() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        MultipartFile file = mock(MultipartFile.class);
        Empleado empleado = new Empleado();
        Estudiante estudiante = new Estudiante();

        EvaluacionCsvRequest fila = new EvaluacionCsvRequest();
        fila.setCodigo("20201001");
        fila.setNivel("PLATINO"); // Nivel inválido

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);
            when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(empleado));

            when(cargaMasivaHelper.procesar(eq(file), eq(EvaluacionCsvRequest.class), any()))
                    .thenAnswer(invocation -> {
                        Consumer<EvaluacionCsvRequest> consumer = invocation.getArgument(2);
                        
                        when(estudianteRepository.findByCodigo("20201001")).thenReturn(Optional.of(estudiante));
                        
                        // Ejecutamos y verificamos la excepción
                        assertThatThrownBy(() -> consumer.accept(fila))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessage("Nivel socioeconómico 'PLATINO' no es válido");
                        
                        return null;
                    });

            // Act
            evaluacionService.procesarCargaMasiva(file);
        }
    }
}
