package pe.com.security.scholarship.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.security.scholarship.domain.entity.Curso;
import pe.com.security.scholarship.domain.entity.Estudiante;
import pe.com.security.scholarship.domain.entity.Matricula;
import pe.com.security.scholarship.domain.entity.Postulacion;
import pe.com.security.scholarship.domain.entity.Seccion;
import pe.com.security.scholarship.domain.enums.EstadoMatricula;
import pe.com.security.scholarship.dto.request.SubmitMatriculaRequest;
import pe.com.security.scholarship.dto.response.IntencionMatriculaResponse;
import pe.com.security.scholarship.dto.response.RegisteredMatriculaResponse;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.repository.CursoRepository;
import pe.com.security.scholarship.repository.EstudianteRepository;
import pe.com.security.scholarship.repository.MatriculaRepository;
import pe.com.security.scholarship.repository.PostulacionRepository;
import pe.com.security.scholarship.repository.SeccionRepository;
import pe.com.security.scholarship.util.SecurityUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
class MatriculaServiceTest {

    @Mock
    private EstudianteRepository estudianteRepository;
    @Mock
    private PostulacionRepository postulacionRepository;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private SeccionRepository seccionRepository;
    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private PostulacionService postulacionService;

    @InjectMocks
    private MatriculaService matriculaService;

    @AfterEach
    void tearDown() {
        // No static mocks to close here as we use try-with-resources
    }

    @Test
    void submitEnrollmentIntention_ShouldSucceed_WhenValidRequest() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        UUID idEstudiante = UUID.randomUUID();
        Integer idSeccion = 1;
        Integer idPostulacion = 100;

        SubmitMatriculaRequest request = new SubmitMatriculaRequest();
        request.setIdSeccion(idSeccion);

        Estudiante estudiante = new Estudiante();
        estudiante.setId(idEstudiante);

        Postulacion postulacion = new Postulacion();
        postulacion.setId(idPostulacion);

        Curso curso = new Curso();
        curso.setId(10);
        curso.setNombre("Curso Test");

        Seccion seccion = new Seccion();
        seccion.setId(idSeccion);
        seccion.setCurso(curso);
        seccion.setFechaInicio(LocalDate.now().plusDays(5)); // Fecha futura
        seccion.setHorarios(new ArrayList<>()); // Inicializar lista para evitar NPE en mapper

        Matricula matriculaSaved = new Matricula();
        matriculaSaved.setId(1);
        matriculaSaved.setPostulacion(postulacion);
        matriculaSaved.setSeccion(seccion);
        matriculaSaved.setEstado(EstadoMatricula.PENDIENTE);
        matriculaSaved.setFechaSolicitud(Instant.now());

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);

            when(estudianteRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(estudiante));
            when(postulacionService.tieneBecaActiva(idEstudiante)).thenReturn(true);
            when(matriculaRepository.existsIntencionMatricula(idEstudiante)).thenReturn(false);
            when(postulacionRepository.findLastPostulacion(idEstudiante)).thenReturn(Optional.of(postulacion));
            when(cursoRepository.findByIdPostulacion(idPostulacion)).thenReturn(List.of(curso));
            when(seccionRepository.findById(idSeccion)).thenReturn(Optional.of(seccion));
            when(matriculaRepository.save(any(Matricula.class))).thenReturn(matriculaSaved);

            // Act
            RegisteredMatriculaResponse response = matriculaService.submitEnrollmentIntention(request);

            // Assert
            assertThat(response).isNotNull();
            verify(matriculaRepository, times(1)).save(any(Matricula.class));
        }
    }

    @Test
    void submitEnrollmentIntention_ShouldThrowBadRequest_WhenScholarshipInactive() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        UUID idEstudiante = UUID.randomUUID();
        SubmitMatriculaRequest request = new SubmitMatriculaRequest();

        Estudiante estudiante = new Estudiante();
        estudiante.setId(idEstudiante);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);

            when(estudianteRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(estudiante));
            when(postulacionService.tieneBecaActiva(idEstudiante)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> matriculaService.submitEnrollmentIntention(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("No tienes una beca vigente");
        }
    }

    @Test
    void submitEnrollmentIntention_ShouldThrowBadRequest_WhenDuplicateIntention() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        UUID idEstudiante = UUID.randomUUID();
        SubmitMatriculaRequest request = new SubmitMatriculaRequest();

        Estudiante estudiante = new Estudiante();
        estudiante.setId(idEstudiante);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);

            when(estudianteRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(estudiante));
            when(postulacionService.tieneBecaActiva(idEstudiante)).thenReturn(true);
            when(matriculaRepository.existsIntencionMatricula(idEstudiante)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> matriculaService.submitEnrollmentIntention(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Ya tienes una intención de matrícula pendiente");
        }
    }

    @Test
    void submitEnrollmentIntention_ShouldThrowBadRequest_WhenSectionInvalidForCourse() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        UUID idEstudiante = UUID.randomUUID();
        Integer idSeccion = 1;
        Integer idPostulacion = 100;

        SubmitMatriculaRequest request = new SubmitMatriculaRequest();
        request.setIdSeccion(idSeccion);

        Estudiante estudiante = new Estudiante();
        estudiante.setId(idEstudiante);

        Postulacion postulacion = new Postulacion();
        postulacion.setId(idPostulacion);

        Curso cursoPostulacion = new Curso();
        cursoPostulacion.setId(10);

        Curso cursoSeccion = new Curso();
        cursoSeccion.setId(20); // Diferente curso

        Seccion seccion = new Seccion();
        seccion.setId(idSeccion);
        seccion.setCurso(cursoSeccion);
        seccion.setFechaInicio(LocalDate.now().plusDays(5));

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);

            when(estudianteRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(estudiante));
            when(postulacionService.tieneBecaActiva(idEstudiante)).thenReturn(true);
            when(matriculaRepository.existsIntencionMatricula(idEstudiante)).thenReturn(false);
            when(postulacionRepository.findLastPostulacion(idEstudiante)).thenReturn(Optional.of(postulacion));
            when(cursoRepository.findByIdPostulacion(idPostulacion)).thenReturn(List.of(cursoPostulacion));
            when(seccionRepository.findById(idSeccion)).thenReturn(Optional.of(seccion));

            // Act & Assert
            assertThatThrownBy(() -> matriculaService.submitEnrollmentIntention(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Sección no válida para la intención de matrícula");
        }
    }

    @Test
    void submitEnrollmentIntention_ShouldThrowBadRequest_WhenDateIsPast() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        UUID idEstudiante = UUID.randomUUID();
        Integer idSeccion = 1;
        Integer idPostulacion = 100;

        SubmitMatriculaRequest request = new SubmitMatriculaRequest();
        request.setIdSeccion(idSeccion);

        Estudiante estudiante = new Estudiante();
        estudiante.setId(idEstudiante);

        Postulacion postulacion = new Postulacion();
        postulacion.setId(idPostulacion);

        Curso curso = new Curso();
        curso.setId(10);

        Seccion seccion = new Seccion();
        seccion.setId(idSeccion);
        seccion.setCurso(curso);
        seccion.setFechaInicio(LocalDate.now().minusDays(1)); // Fecha pasada

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);

            when(estudianteRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(estudiante));
            when(postulacionService.tieneBecaActiva(idEstudiante)).thenReturn(true);
            when(matriculaRepository.existsIntencionMatricula(idEstudiante)).thenReturn(false);
            when(postulacionRepository.findLastPostulacion(idEstudiante)).thenReturn(Optional.of(postulacion));
            when(cursoRepository.findByIdPostulacion(idPostulacion)).thenReturn(List.of(curso));
            when(seccionRepository.findById(idSeccion)).thenReturn(Optional.of(seccion));

            // Act & Assert
            assertThatThrownBy(() -> matriculaService.submitEnrollmentIntention(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Sección no válida para la intención de matrícula");
        }
    }

    @Test
    void getIntencionMatricula_ShouldReturnResponse_WhenMatriculaExists() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        UUID idEstudiante = UUID.randomUUID();

        Estudiante estudiante = new Estudiante();
        estudiante.setId(idEstudiante);

        Curso curso = new Curso();
        curso.setNombre("Java Avanzado");

        Seccion seccion = new Seccion();
        seccion.setCurso(curso);
        seccion.setFechaInicio(LocalDate.now().plusDays(10));
        seccion.setHorarios(Collections.emptyList()); // Inicializar lista para evitar NPE

        Matricula matricula = new Matricula();
        matricula.setId(1);
        matricula.setSeccion(seccion);
        matricula.setFechaSolicitud(Instant.now()); // Inicializar fecha para evitar NPE
        matricula.setEstado(EstadoMatricula.PENDIENTE);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);

            when(estudianteRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(estudiante));
            when(matriculaRepository.findMatriculaByIdEstudiante(idEstudiante)).thenReturn(Optional.of(matricula));

            // Act
            IntencionMatriculaResponse response = matriculaService.getIntencionMatricula();

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getNombreCurso()).isEqualTo("Java Avanzado");
        }
    }

    @Test
    void getIntencionMatricula_ShouldThrowNotFound_WhenMatriculaDoesNotExist() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        UUID idEstudiante = UUID.randomUUID();

        Estudiante estudiante = new Estudiante();
        estudiante.setId(idEstudiante);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);

            when(estudianteRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(estudiante));
            when(matriculaRepository.findMatriculaByIdEstudiante(idEstudiante)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> matriculaService.getIntencionMatricula())
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("No se encontró estudiante asociado al id del payload");
        }
    }
}
