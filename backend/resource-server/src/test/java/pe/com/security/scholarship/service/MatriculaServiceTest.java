package pe.com.security.scholarship.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import pe.com.security.scholarship.domain.entity.Curso;
import pe.com.security.scholarship.domain.entity.Empleado;
import pe.com.security.scholarship.domain.entity.Estudiante;
import pe.com.security.scholarship.domain.entity.Matricula;
import pe.com.security.scholarship.domain.entity.Postulacion;
import pe.com.security.scholarship.domain.entity.Seccion;
import pe.com.security.scholarship.domain.enums.EstadoMatricula;
import pe.com.security.scholarship.dto.projection.BecadoIntencionProjection;
import pe.com.security.scholarship.dto.projection.SeccionIntencionProjection;
import pe.com.security.scholarship.dto.request.AprobarMatriculaRequest;
import pe.com.security.scholarship.dto.request.SubmitMatriculaRequest;
import pe.com.security.scholarship.dto.response.CursoIntencionMatriculaResponse;
import pe.com.security.scholarship.dto.response.IntencionMatriculaResponse;
import pe.com.security.scholarship.dto.response.RegisteredMatriculaResponse;
import pe.com.security.scholarship.dto.response.SeccionBecadosResponse;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.repository.CursoRepository;
import pe.com.security.scholarship.repository.EmpleadoRepository;
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
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock
    private EmpleadoRepository empleadoRepository;

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
            // Actualización: ahora devuelve una lista
            when(matriculaRepository.findLastMatriculaByIdEstudiante(eq(idEstudiante), any(Limit.class)))
                    .thenReturn(List.of(matricula));

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
            // Actualización: ahora devuelve una lista vacía
            when(matriculaRepository.findLastMatriculaByIdEstudiante(eq(idEstudiante), any(Limit.class)))
                    .thenReturn(Collections.emptyList());

            // Act & Assert
            assertThatThrownBy(() -> matriculaService.getIntencionMatricula())
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("No se encontró matrícula asociada al estudiante");
        }
    }

    @Test
    void getIntencionesMatriculaSeccion_ShouldReturnGroupedAndSortedList() {
        // Arrange
        SeccionIntencionProjection proj1 = new RealProjection(101, LocalDate.now().plusDays(5), 1, "Curso A", "C001", 10);
        SeccionIntencionProjection proj2 = new RealProjection(102, LocalDate.now().plusDays(10), 1, "Curso A", "C001", 5);
        SeccionIntencionProjection proj3 = new RealProjection(201, LocalDate.now().plusDays(2), 2, "Curso B", "C002", 8);

        when(matriculaRepository.findIntencionesMatriculaSeccion())
                .thenReturn(List.of(proj1, proj2, proj3));

        // Act
        List<CursoIntencionMatriculaResponse> result = matriculaService.getIntencionesMatriculaSeccion();

        // Assert
        assertThat(result).hasSize(2); // 2 cursos únicos

        // Verificar ordenamiento: Curso B debe ir primero porque su sección inicia antes
        assertThat(result.get(0).getIdCurso()).isEqualTo(2);
        assertThat(result.get(1).getIdCurso()).isEqualTo(1);

        // Verificar agrupación
        CursoIntencionMatriculaResponse cursoA = result.stream()
                .filter(c -> c.getIdCurso() == 1)
                .findFirst()
                .orElseThrow();
        assertThat(cursoA.getSecciones()).hasSize(2); // 2 secciones para Curso A
    }

    @Test
    void getIntencionesMatriculaSeccion_ShouldReturnEmptyList_WhenNoData() {
        // Arrange
        when(matriculaRepository.findIntencionesMatriculaSeccion()).thenReturn(Collections.emptyList());

        // Act
        List<CursoIntencionMatriculaResponse> result = matriculaService.getIntencionesMatriculaSeccion();

        // Assert
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void getBecadosSeccion_ShouldReturnResponse_WhenSectionExistsAndDataAvailable() {
        // Arrange
        Integer idSeccion = 1;
        Seccion seccion = new Seccion();
        seccion.setId(idSeccion);
        seccion.setFechaInicio(LocalDate.now().plusDays(5));
        seccion.setVacantesDisponibles(20);

        BecadoIntencionProjection proj1 = new RealBecadoProjection(100, "Juan Perez", "S001", 18.5, EstadoMatricula.PENDIENTE);
        BecadoIntencionProjection proj2 = new RealBecadoProjection(101, "Maria Lopez", "S002", 19.0, EstadoMatricula.ACEPTADO);

        when(seccionRepository.findById(idSeccion)).thenReturn(Optional.of(seccion));
        when(seccionRepository.getVacantesRestantes(idSeccion)).thenReturn(15);
        when(matriculaRepository.findBecadosIntencionMatricula(idSeccion)).thenReturn(List.of(proj1, proj2));

        // Act
        SeccionBecadosResponse result = matriculaService.getBecadosSeccion(idSeccion);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getIdSeccion()).isEqualTo(idSeccion);
        assertThat(result.getVacantesTotales()).isEqualTo(20);
        assertThat(result.getVacantesDisponibles()).isEqualTo(15);
        assertThat(result.getBecados()).hasSize(2);
        assertThat(result.getBecados().get(0).getNombreCompleto()).isEqualTo("Juan Perez");
    }

    @Test
    void getBecadosSeccion_ShouldThrowNotFound_WhenSectionDoesNotExist() {
        // Arrange
        Integer idSeccion = 999;
        when(seccionRepository.findById(idSeccion)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> matriculaService.getBecadosSeccion(idSeccion))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("No se encontró la sección con el ID enviado");
    }

    @Test
    void getBecadosSeccion_ShouldReturnEmptyList_WhenSectionExistsButNoData() {
        // Arrange
        Integer idSeccion = 1;
        Seccion seccion = new Seccion();
        seccion.setId(idSeccion);
        seccion.setVacantesDisponibles(20);

        when(seccionRepository.findById(idSeccion)).thenReturn(Optional.of(seccion));
        when(seccionRepository.getVacantesRestantes(idSeccion)).thenReturn(20);
        when(matriculaRepository.findBecadosIntencionMatricula(idSeccion)).thenReturn(Collections.emptyList());

        // Act
        SeccionBecadosResponse result = matriculaService.getBecadosSeccion(idSeccion);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getBecados()).isEmpty();
        assertThat(result.getVacantesDisponibles()).isEqualTo(20);
    }

    @Test
    void actualizarEstadoMatricula_ShouldApprove_WhenValidRequest() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        Integer idMatricula = 1;
        AprobarMatriculaRequest request = new AprobarMatriculaRequest();
        request.setIdMatricula(idMatricula);
        request.setAprobado(true);

        Empleado empleado = new Empleado();
        empleado.setId(UUID.randomUUID());

        Matricula matricula = new Matricula();
        matricula.setId(idMatricula);
        matricula.setEstado(EstadoMatricula.PENDIENTE);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);

            when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(empleado));
            when(matriculaRepository.findById(idMatricula)).thenReturn(Optional.of(matricula));

            // Act
            matriculaService.actualizarEstadoMatricula(request);

            // Assert
            assertThat(matricula.getEstado()).isEqualTo(EstadoMatricula.ACEPTADO);
            assertThat(matricula.getFechaMatricula()).isNotNull();
            assertThat(matricula.getEmpleado()).isEqualTo(empleado);
        }
    }

    @Test
    void actualizarEstadoMatricula_ShouldReject_WhenValidRequest() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        Integer idMatricula = 1;
        AprobarMatriculaRequest request = new AprobarMatriculaRequest();
        request.setIdMatricula(idMatricula);
        request.setAprobado(false);

        Empleado empleado = new Empleado();
        empleado.setId(UUID.randomUUID());

        Matricula matricula = new Matricula();
        matricula.setId(idMatricula);
        matricula.setEstado(EstadoMatricula.PENDIENTE);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);

            when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(empleado));
            when(matriculaRepository.findById(idMatricula)).thenReturn(Optional.of(matricula));

            // Act
            matriculaService.actualizarEstadoMatricula(request);

            // Assert
            assertThat(matricula.getEstado()).isEqualTo(EstadoMatricula.RECHAZADO);
            assertThat(matricula.getFechaMatricula()).isNull();
            assertThat(matricula.getEmpleado()).isEqualTo(empleado);
        }
    }

    @Test
    void actualizarEstadoMatricula_ShouldThrowBadRequest_WhenStateIsRepeated() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        Integer idMatricula = 1;
        AprobarMatriculaRequest request = new AprobarMatriculaRequest();
        request.setIdMatricula(idMatricula);
        request.setAprobado(true);

        Empleado empleado = new Empleado();
        Matricula matricula = new Matricula();
        matricula.setId(idMatricula);
        matricula.setEstado(EstadoMatricula.ACEPTADO);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);

            when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(empleado));
            when(matriculaRepository.findById(idMatricula)).thenReturn(Optional.of(matricula));

            // Act & Assert
            assertThatThrownBy(() -> matriculaService.actualizarEstadoMatricula(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("No se puede repetir el mismo estado");
        }
    }

    @Test
    void actualizarEstadoMatricula_ShouldThrowBadRequest_WhenRejectingApprovedMatricula() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        Integer idMatricula = 1;
        AprobarMatriculaRequest request = new AprobarMatriculaRequest();
        request.setIdMatricula(idMatricula);
        request.setAprobado(false);

        Empleado empleado = new Empleado();
        Matricula matricula = new Matricula();
        matricula.setId(idMatricula);
        matricula.setEstado(EstadoMatricula.ACEPTADO);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);

            when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(empleado));
            when(matriculaRepository.findById(idMatricula)).thenReturn(Optional.of(matricula));

            // Act & Assert
            assertThatThrownBy(() -> matriculaService.actualizarEstadoMatricula(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("No se puede rechazar una matrícula ya aprobada");
        }
    }

    @Test
    void actualizarEstadoMatricula_ShouldThrowNotFound_WhenEmployeeNotFound() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        AprobarMatriculaRequest request = new AprobarMatriculaRequest();

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);

            when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> matriculaService.actualizarEstadoMatricula(request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("No se encontró empleado con el ID del payload");
        }
    }

    @Test
    void actualizarEstadoMatricula_ShouldThrowNotFound_WhenMatriculaNotFound() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        Integer idMatricula = 1;
        AprobarMatriculaRequest request = new AprobarMatriculaRequest();
        request.setIdMatricula(idMatricula);

        Empleado empleado = new Empleado();

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);

            when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(empleado));
            when(matriculaRepository.findById(idMatricula)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> matriculaService.actualizarEstadoMatricula(request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("No se encontró la matrícula con el ID enviado");
        }
    }

    static class RealProjection implements SeccionIntencionProjection {
        private final Integer idSeccion;
        private final LocalDate fechaInicio;
        private final Integer idCurso;
        private final String nombreCurso;
        private final String codigoCurso;
        private final Integer totalIntencionesMatricula;

        public RealProjection(Integer idSeccion, LocalDate fechaInicio, Integer idCurso, String nombreCurso, String codigoCurso, Integer totalIntencionesMatricula) {
            this.idSeccion = idSeccion;
            this.fechaInicio = fechaInicio;
            this.idCurso = idCurso;
            this.nombreCurso = nombreCurso;
            this.codigoCurso = codigoCurso;
            this.totalIntencionesMatricula = totalIntencionesMatricula;
        }

        @Override
        public Integer getIdSeccion() { return idSeccion; }

        @Override
        public LocalDate getFechaInicio() { return fechaInicio; }

        @Override
        public Integer getIdCurso() { return idCurso; }

        @Override
        public String getNombreCurso() { return nombreCurso; }

        @Override
        public String getCodigoCurso() { return codigoCurso; }

        @Override
        public Integer getTotalIntencionesMatricula() { return totalIntencionesMatricula; }
    }

    static class RealBecadoProjection implements BecadoIntencionProjection {
        private final Integer idMatricula;
        private final String nombreCompleto;
        private final String codigo;
        private final Double promedioGeneral;
        private final EstadoMatricula estadoMatricula;

        public RealBecadoProjection(Integer idMatricula, String nombreCompleto, String codigo, Double promedioGeneral, EstadoMatricula estadoMatricula) {
            this.idMatricula = idMatricula;
            this.nombreCompleto = nombreCompleto;
            this.codigo = codigo;
            this.promedioGeneral = promedioGeneral;
            this.estadoMatricula = estadoMatricula;
        }

        @Override
        public Integer getIdMatricula() { return idMatricula; }

        @Override
        public String getNombreCompleto() { return nombreCompleto; }

        @Override
        public String getCodigo() { return codigo; }

        @Override
        public Double getPromedioGeneral() { return promedioGeneral; }

        @Override
        public EstadoMatricula getEstadoMatricula() { return estadoMatricula; }
    }
}
