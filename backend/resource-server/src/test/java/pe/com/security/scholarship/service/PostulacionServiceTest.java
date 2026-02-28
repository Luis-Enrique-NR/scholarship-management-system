package pe.com.security.scholarship.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import pe.com.security.scholarship.domain.entity.Convocatoria;
import pe.com.security.scholarship.domain.entity.Curso;
import pe.com.security.scholarship.domain.entity.Estudiante;
import pe.com.security.scholarship.domain.entity.Matricula;
import pe.com.security.scholarship.domain.entity.Postulacion;
import pe.com.security.scholarship.domain.entity.Seccion;
import pe.com.security.scholarship.domain.enums.EstadoConvocatoria;
import pe.com.security.scholarship.domain.enums.EstadoMatricula;
import pe.com.security.scholarship.domain.enums.Mes;
import pe.com.security.scholarship.domain.enums.ModalidadCurso;
import pe.com.security.scholarship.dto.projection.IdentificacionEstudianteProjection;
import pe.com.security.scholarship.dto.projection.PostulanteConvocatoriaProjection;
import pe.com.security.scholarship.dto.request.RegisterPostulacionRequest;
import pe.com.security.scholarship.dto.response.ConsultaPostulacionResponse;
import pe.com.security.scholarship.dto.response.DetallePostulanteResponse;
import pe.com.security.scholarship.dto.response.HistorialPostulacionResponse;
import pe.com.security.scholarship.dto.response.PostulanteConvocatoriaResponse;
import pe.com.security.scholarship.dto.response.RegisteredPostulacionResponse;
import pe.com.security.scholarship.dto.response.ResultadoPostulacionResponse;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.repository.ConvocatoriaRepository;
import pe.com.security.scholarship.repository.CursoRepository;
import pe.com.security.scholarship.repository.EstudianteRepository;
import pe.com.security.scholarship.repository.MatriculaRepository;
import pe.com.security.scholarship.repository.PostulacionRepository;
import pe.com.security.scholarship.util.SecurityUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Test
    void getDetallePostulacion_ShouldReturnResponse_WhenPostulacionExists() {
        // Arrange
        Integer idPostulacion = 1;
        Postulacion postulacion = new Postulacion();
        postulacion.setId(idPostulacion);
        
        Convocatoria convocatoria = new Convocatoria();
        convocatoria.setMes(Mes.ENERO);
        convocatoria.setEstado(EstadoConvocatoria.APERTURADO);
        postulacion.setConvocatoria(convocatoria);
        
        Estudiante estudiante = new Estudiante();
        postulacion.setEstudiante(estudiante);
        
        postulacion.setCursos(Set.of());

        when(postulacionRepository.findById(idPostulacion)).thenReturn(Optional.of(postulacion));

        // Act
        ConsultaPostulacionResponse response = postulacionService.getDetallePostulacion(idPostulacion);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(idPostulacion);
    }

    @Test
    void getDetallePostulacion_ShouldThrowNotFound_WhenPostulacionDoesNotExist() {
        // Arrange
        Integer idPostulacion = 1;
        when(postulacionRepository.findById(idPostulacion)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> postulacionService.getDetallePostulacion(idPostulacion))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("No se encontró la postulación");
    }

    @Test
    void getHistorialPostulacion_ShouldReturnList_WhenDataExists() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        UUID idEstudiante = UUID.randomUUID();
        Integer year = 2023;

        Estudiante estudiante = new Estudiante();
        estudiante.setId(idEstudiante);

        Postulacion postulacion = new Postulacion();
        postulacion.setId(1);
        Convocatoria convocatoria = new Convocatoria();
        convocatoria.setMes(Mes.ENERO);
        postulacion.setConvocatoria(convocatoria);
        postulacion.setCursos(Set.of());

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);

            when(estudianteRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(estudiante));
            when(postulacionRepository.findByYear(idEstudiante, year)).thenReturn(List.of(postulacion));

            // Act
            List<HistorialPostulacionResponse> response = postulacionService.getHistorialPostulacion(year);

            // Assert
            assertThat(response).isNotEmpty();
            assertThat(response).hasSize(1);
        }
    }

    @Test
    void getHistorialPostulacion_ShouldReturnEmptyList_WhenNoDataExists() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        UUID idEstudiante = UUID.randomUUID();
        Integer year = 2023;

        Estudiante estudiante = new Estudiante();
        estudiante.setId(idEstudiante);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);

            when(estudianteRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(estudiante));
            when(postulacionRepository.findByYear(idEstudiante, year)).thenReturn(Collections.emptyList());

            // Act
            List<HistorialPostulacionResponse> response = postulacionService.getHistorialPostulacion(year);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response).isEmpty();
        }
    }

    @Test
    void getHistorialPostulacion_ShouldThrowNotFound_WhenEstudianteDoesNotExist() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        Integer year = 2023;

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);

            when(estudianteRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> postulacionService.getHistorialPostulacion(year))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("No se encontró estudiante asociado al id del payload");
        }
    }

    @Test
    void obtenerPostulantesConvocatoria_ShouldReturnPage_WhenSortIsValid() {
        // Arrange
        Integer idConvocatoria = 1;
        Pageable pageable = PageRequest.of(0, 10, Sort.by("promedioGeneral"));
        
        // Mock de la proyección
        PostulanteConvocatoriaProjection projection = mock(PostulanteConvocatoriaProjection.class);
        when(projection.getIdEstudiante()).thenReturn(UUID.randomUUID());
        when(projection.getCodigo()).thenReturn("STU001");
        when(projection.getNombreCompleto()).thenReturn("Juan Perez");
        when(projection.getBecado()).thenReturn(true);
        when(projection.getPromedioGeneral()).thenReturn(18.5);
        when(projection.getFechaPostulacion()).thenReturn(LocalDate.now());

        Page<PostulanteConvocatoriaProjection> page = new PageImpl<>(List.of(projection));

        when(postulacionRepository.buscarPostulantesConvocatoria(eq(idConvocatoria), any(Pageable.class)))
                .thenReturn(page);

        // Act
        Page<PostulanteConvocatoriaResponse> result = postulacionService.obtenerPostulantesConvocatoria(idConvocatoria, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        
        PostulanteConvocatoriaResponse response = result.getContent().get(0);
        assertThat(response.getNombreCompleto()).isEqualTo("Juan Perez");
        assertThat(response.getCodigo()).isEqualTo("STU001");
        assertThat(response.getBecado()).isTrue();
        assertThat(response.getPromedioGeneral()).isEqualTo(18.5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(postulacionRepository).buscarPostulantesConvocatoria(eq(idConvocatoria), pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();
        Sort.Order order = capturedPageable.getSort().getOrderFor("promedioGeneral");
        assertThat(order).isNotNull();
        assertThat(order.getNullHandling()).isEqualTo(Sort.NullHandling.NULLS_LAST);
    }

    @Test
    void obtenerPostulantesConvocatoria_ShouldThrowBadRequest_WhenSortIsInvalid() {
        // Arrange
        Integer idConvocatoria = 1;
        Pageable pageable = PageRequest.of(0, 10, Sort.by("contraseña"));

        // Act & Assert
        assertThatThrownBy(() -> postulacionService.obtenerPostulantesConvocatoria(idConvocatoria, pageable))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("No se puede ordenar por el campo: contraseña");

        verifyNoInteractions(postulacionRepository);
    }

    @Test
    void getPostulacionesEstudiante_ShouldReturnResponse_WhenStudentExistsAndHasPostulaciones() {
        // Arrange
        UUID idEstudiante = UUID.randomUUID();
        Integer year = 2023;
        String codigoEstudiante = "C12345";
        String nombreCompleto = "Juan Perez";

        IdentificacionEstudianteProjection projection = mock(IdentificacionEstudianteProjection.class);
        when(projection.getCodigoEstudiante()).thenReturn(codigoEstudiante);
        when(projection.getNombreCompleto()).thenReturn(nombreCompleto);

        Convocatoria convocatoria = new Convocatoria();
        convocatoria.setMes(Mes.ENERO);

        Curso curso = new Curso();
        curso.setNombre("Java Basics");

        Seccion seccion = new Seccion();
        seccion.setCurso(curso);

        Matricula matricula = new Matricula();
        matricula.setFechaMatricula(java.time.Instant.now());
        matricula.setSeccion(seccion);
        matricula.setNota(18.0);

        Postulacion postulacion = new Postulacion();
        postulacion.setConvocatoria(convocatoria);
        postulacion.setFechaPostulacion(java.time.LocalDate.now());
        postulacion.setPromedioGeneral(15.0);
        postulacion.setAceptado(true);
        postulacion.setCursos(Set.of(curso));
        postulacion.setMatriculas(List.of(matricula));

        when(estudianteRepository.findDatosEstudiante(idEstudiante)).thenReturn(Optional.of(projection));
        when(postulacionRepository.findByYearWithMatricula(idEstudiante, year, EstadoMatricula.ACEPTADO))
                .thenReturn(List.of(postulacion));

        // Act
        DetallePostulanteResponse response = postulacionService.getPostulacionesEstudiante(idEstudiante, year);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getIdEstudiante()).isEqualTo(idEstudiante);
        assertThat(response.getCodigoEstudiante()).isEqualTo(codigoEstudiante);
        assertThat(response.getNombreCompleto()).isEqualTo(nombreCompleto);
        assertThat(response.getPostulaciones()).hasSize(1);

        ResultadoPostulacionResponse resultado = response.getPostulaciones().get(0);
        assertThat(resultado.getPostulacion().getMesConvocatoria()).isEqualTo("ENERO");
        assertThat(resultado.getMatricula().getCursoMatriculado()).isEqualTo("Java Basics");
    }

    @Test
    void getPostulacionesEstudiante_ShouldThrowNotFound_WhenStudentDoesNotExist() {
        // Arrange
        UUID idEstudiante = UUID.randomUUID();
        Integer year = 2023;

        when(estudianteRepository.findDatosEstudiante(idEstudiante)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> postulacionService.getPostulacionesEstudiante(idEstudiante, year))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("No existe estudiante con el ID ingresado");

        verify(postulacionRepository, times(0)).findByYearWithMatricula(any(), any(), any());
    }

    @Test
    void getPostulacionesEstudiante_ShouldReturnEmptyList_WhenStudentHasNoPostulaciones() {
        // Arrange
        UUID idEstudiante = UUID.randomUUID();
        Integer year = 2023;
        String codigoEstudiante = "C12345";
        String nombreCompleto = "Juan Perez";

        IdentificacionEstudianteProjection projection = mock(IdentificacionEstudianteProjection.class);
        when(projection.getCodigoEstudiante()).thenReturn(codigoEstudiante);
        when(projection.getNombreCompleto()).thenReturn(nombreCompleto);

        when(estudianteRepository.findDatosEstudiante(idEstudiante)).thenReturn(Optional.of(projection));
        when(postulacionRepository.findByYearWithMatricula(idEstudiante, year, EstadoMatricula.ACEPTADO))
                .thenReturn(Collections.emptyList());

        // Act
        DetallePostulanteResponse response = postulacionService.getPostulacionesEstudiante(idEstudiante, year);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getIdEstudiante()).isEqualTo(idEstudiante);
        assertThat(response.getPostulaciones()).isNotNull().isEmpty();
    }
}
