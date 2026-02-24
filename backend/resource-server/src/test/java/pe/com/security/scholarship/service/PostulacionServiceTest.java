package pe.com.security.scholarship.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.security.scholarship.domain.entity.Convocatoria;
import pe.com.security.scholarship.domain.entity.Curso;
import pe.com.security.scholarship.domain.entity.Estudiante;
import pe.com.security.scholarship.domain.entity.Postulacion;
import pe.com.security.scholarship.domain.enums.ModalidadCurso;
import pe.com.security.scholarship.dto.request.RegisterPostulacionRequest;
import pe.com.security.scholarship.dto.response.RegisteredPostulacionResponse;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.repository.ConvocatoriaRepository;
import pe.com.security.scholarship.repository.CursoRepository;
import pe.com.security.scholarship.repository.EstudianteRepository;
import pe.com.security.scholarship.repository.MatriculaRepository;
import pe.com.security.scholarship.repository.PostulacionRepository;
import pe.com.security.scholarship.util.SecurityUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostulacionServiceTest {

    @Mock
    private PostulacionRepository postulacionRepository;
    @Mock
    private ConvocatoriaRepository convocatoriaRepository;
    @Mock
    private EstudianteRepository estudianteRepository;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private MatriculaRepository matriculaRepository;

    @InjectMocks
    private PostulacionService postulacionService;

    @AfterEach
    void tearDown() {
        // No static mocks to close here as we use try-with-resources
    }

    @Test
    void registerPostulacion_ShouldSucceed_WhenFirstScholarshipOfYear() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        UUID idEstudiante = UUID.randomUUID();
        Integer idConvocatoria = 1;
        List<Integer> idsCursos = List.of(1, 2);

        RegisterPostulacionRequest request = new RegisterPostulacionRequest();
        request.setIdConvocatoria(idConvocatoria);
        request.setIdsCursos(idsCursos);

        Convocatoria convocatoria = new Convocatoria();
        convocatoria.setId(idConvocatoria);

        Estudiante estudiante = new Estudiante();
        estudiante.setId(idEstudiante);

        Curso curso1 = new Curso(); curso1.setId(1); curso1.setModalidad(ModalidadCurso.HIBRIDO);
        Curso curso2 = new Curso(); curso2.setId(2); curso2.setModalidad(ModalidadCurso.PRESENCIAL);
        List<Curso> cursos = List.of(curso1, curso2);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);

            when(convocatoriaRepository.findById(idConvocatoria)).thenReturn(Optional.of(convocatoria));
            when(estudianteRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(estudiante));
            when(postulacionRepository.existsByEstudianteIdAndConvocatoriaId(idEstudiante, idConvocatoria)).thenReturn(false);
            
            // Primera beca del año
            when(postulacionRepository.cantidadBecasByYear(idEstudiante)).thenReturn(0);
            
            when(cursoRepository.findAllById(idsCursos)).thenReturn(cursos);
            when(postulacionRepository.save(any(Postulacion.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            RegisteredPostulacionResponse response = postulacionService.registerPostulacion(request);

            // Assert
            assertThat(response).isNotNull();
            verify(postulacionRepository, times(1)).save(any(Postulacion.class));
            // Verify validations were skipped
            verify(matriculaRepository, times(0)).notaUltimaMatricula(any());
        }
    }

    @Test
    void registerPostulacion_ShouldThrowBadRequest_WhenScholarshipLimitReached() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        UUID idEstudiante = UUID.randomUUID();
        Integer idConvocatoria = 1;

        RegisterPostulacionRequest request = new RegisterPostulacionRequest();
        request.setIdConvocatoria(idConvocatoria);

        Convocatoria convocatoria = new Convocatoria();
        convocatoria.setId(idConvocatoria);
        Estudiante estudiante = new Estudiante();
        estudiante.setId(idEstudiante);
        
        Postulacion lastPostulacion = new Postulacion();
        lastPostulacion.setId(100);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);

            when(convocatoriaRepository.findById(idConvocatoria)).thenReturn(Optional.of(convocatoria));
            when(estudianteRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(estudiante));
            when(postulacionRepository.existsByEstudianteIdAndConvocatoriaId(idEstudiante, idConvocatoria)).thenReturn(false);
            
            // Tiene becas previas
            when(postulacionRepository.cantidadBecasByYear(idEstudiante)).thenReturn(3);
            
            // Mock tieneBecaActiva -> false (para llegar a la validación de límite)
            when(postulacionRepository.findLastPostulacion(idEstudiante)).thenReturn(Optional.of(lastPostulacion));
            when(matriculaRepository.seMatriculo(lastPostulacion.getId())).thenReturn(true); // Ya se matriculó, no tiene beca vigente por tiempo

            // Act & Assert
            assertThatThrownBy(() -> postulacionService.registerPostulacion(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Ya alcanzaste el límite de 3 becas por año");
        }
    }

    @Test
    void registerPostulacion_ShouldThrowBadRequest_WhenMinimumGradeNotMet() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        UUID idEstudiante = UUID.randomUUID();
        Integer idConvocatoria = 1;

        RegisterPostulacionRequest request = new RegisterPostulacionRequest();
        request.setIdConvocatoria(idConvocatoria);

        Convocatoria convocatoria = new Convocatoria();
        convocatoria.setId(idConvocatoria);
        Estudiante estudiante = new Estudiante();
        estudiante.setId(idEstudiante);
        
        Postulacion lastPostulacion = new Postulacion();
        lastPostulacion.setId(100);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);

            when(convocatoriaRepository.findById(idConvocatoria)).thenReturn(Optional.of(convocatoria));
            when(estudianteRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(estudiante));
            when(postulacionRepository.existsByEstudianteIdAndConvocatoriaId(idEstudiante, idConvocatoria)).thenReturn(false);
            
            // Tiene 1 beca previa
            when(postulacionRepository.cantidadBecasByYear(idEstudiante)).thenReturn(1);
            
            // Mock tieneBecaActiva -> false
            when(postulacionRepository.findLastPostulacion(idEstudiante)).thenReturn(Optional.of(lastPostulacion));
            when(matriculaRepository.seMatriculo(lastPostulacion.getId())).thenReturn(true);

            // Nota menor a 15
            when(matriculaRepository.notaUltimaMatricula(idEstudiante)).thenReturn(14.0);

            // Act & Assert
            assertThatThrownBy(() -> postulacionService.registerPostulacion(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Tu última nota debe ser mínima 15");
        }
    }

    @Test
    void registerPostulacion_ShouldThrowBadRequest_WhenScholarshipIsActive_ScenarioA() {
        // Escenario A: No se matriculó y tiene menos de 3 meses
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        UUID idEstudiante = UUID.randomUUID();
        Integer idConvocatoria = 1;

        RegisterPostulacionRequest request = new RegisterPostulacionRequest();
        request.setIdConvocatoria(idConvocatoria);

        Convocatoria convocatoria = new Convocatoria();
        convocatoria.setId(idConvocatoria);
        Estudiante estudiante = new Estudiante();
        estudiante.setId(idEstudiante);
        
        Postulacion lastPostulacion = new Postulacion();
        lastPostulacion.setId(100);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);

            when(convocatoriaRepository.findById(idConvocatoria)).thenReturn(Optional.of(convocatoria));
            when(estudianteRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(estudiante));
            when(postulacionRepository.existsByEstudianteIdAndConvocatoriaId(idEstudiante, idConvocatoria)).thenReturn(false);
            when(postulacionRepository.cantidadBecasByYear(idEstudiante)).thenReturn(1);

            // Lógica tieneBecaActiva
            when(postulacionRepository.findLastPostulacion(idEstudiante)).thenReturn(Optional.of(lastPostulacion));
            when(matriculaRepository.seMatriculo(lastPostulacion.getId())).thenReturn(false); // No se matriculó
            when(postulacionRepository.cantidadMesesBeca(idEstudiante)).thenReturn(2); // Menos de 3 meses

            // Act & Assert
            assertThatThrownBy(() -> postulacionService.registerPostulacion(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Aún tienes una beca vigente");
        }
    }

    @Test
    void registerPostulacion_ShouldSucceed_WhenScholarshipIsActive_ScenarioB() {
        // Escenario B: Ya se matriculó (debe permitir postular aunque < 3 meses)
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        UUID idEstudiante = UUID.randomUUID();
        Integer idConvocatoria = 1;
        List<Integer> idsCursos = List.of(1);

        RegisterPostulacionRequest request = new RegisterPostulacionRequest();
        request.setIdConvocatoria(idConvocatoria);
        request.setIdsCursos(idsCursos);

        Convocatoria convocatoria = new Convocatoria();
        convocatoria.setId(idConvocatoria);
        Estudiante estudiante = new Estudiante();
        estudiante.setId(idEstudiante);
        
        Postulacion lastPostulacion = new Postulacion();
        lastPostulacion.setId(100);
        
        Curso curso = new Curso(); curso.setId(1); curso.setModalidad(ModalidadCurso.HIBRIDO);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);

            when(convocatoriaRepository.findById(idConvocatoria)).thenReturn(Optional.of(convocatoria));
            when(estudianteRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(estudiante));
            when(postulacionRepository.existsByEstudianteIdAndConvocatoriaId(idEstudiante, idConvocatoria)).thenReturn(false);
            when(postulacionRepository.cantidadBecasByYear(idEstudiante)).thenReturn(1);

            // Lógica tieneBecaActiva -> false porque ya se matriculó
            when(postulacionRepository.findLastPostulacion(idEstudiante)).thenReturn(Optional.of(lastPostulacion));
            when(matriculaRepository.seMatriculo(lastPostulacion.getId())).thenReturn(true); 
            
            // Nota aprobatoria
            when(matriculaRepository.notaUltimaMatricula(idEstudiante)).thenReturn(16.0);
            
            when(cursoRepository.findAllById(idsCursos)).thenReturn(List.of(curso));
            when(postulacionRepository.save(any(Postulacion.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            RegisteredPostulacionResponse response = postulacionService.registerPostulacion(request);

            // Assert
            assertThat(response).isNotNull();
            verify(postulacionRepository, times(1)).save(any(Postulacion.class));
        }
    }

    @Test
    void registerPostulacion_ShouldThrowNotFound_WhenConvocatoriaDoesNotExist() {
        // Arrange
        RegisterPostulacionRequest request = new RegisterPostulacionRequest();
        request.setIdConvocatoria(1);
        when(convocatoriaRepository.findById(1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> postulacionService.registerPostulacion(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("No se encontró la convocatoria");
    }

    @Test
    void registerPostulacion_ShouldThrowNotFound_WhenEstudianteDoesNotExist() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        RegisterPostulacionRequest request = new RegisterPostulacionRequest();
        request.setIdConvocatoria(1);
        
        Convocatoria convocatoria = new Convocatoria();
        convocatoria.setId(1);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);
            
            when(convocatoriaRepository.findById(1)).thenReturn(Optional.of(convocatoria));
            when(estudianteRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> postulacionService.registerPostulacion(request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("No se encontró estudiante con el user id del payload");
        }
    }

    @Test
    void registerPostulacion_ShouldThrowNotFound_WhenCoursesAreIncomplete() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        UUID idEstudiante = UUID.randomUUID();
        Integer idConvocatoria = 1;
        List<Integer> idsCursos = List.of(1, 2); // Pide 2 cursos

        RegisterPostulacionRequest request = new RegisterPostulacionRequest();
        request.setIdConvocatoria(idConvocatoria);
        request.setIdsCursos(idsCursos);

        Convocatoria convocatoria = new Convocatoria();
        convocatoria.setId(idConvocatoria);
        Estudiante estudiante = new Estudiante();
        estudiante.setId(idEstudiante);

        Curso curso1 = new Curso(); curso1.setId(1); curso1.setModalidad(ModalidadCurso.HIBRIDO);
        // Solo encuentra 1 curso
        List<Curso> cursosEncontrados = List.of(curso1);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);

            when(convocatoriaRepository.findById(idConvocatoria)).thenReturn(Optional.of(convocatoria));
            when(estudianteRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(estudiante));
            when(postulacionRepository.existsByEstudianteIdAndConvocatoriaId(idEstudiante, idConvocatoria)).thenReturn(false);
            when(postulacionRepository.cantidadBecasByYear(idEstudiante)).thenReturn(0);
            
            when(cursoRepository.findAllById(idsCursos)).thenReturn(cursosEncontrados);

            // Act & Assert
            assertThatThrownBy(() -> postulacionService.registerPostulacion(request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Uno o más cursos no fueron encontrados");
        }
    }
}
