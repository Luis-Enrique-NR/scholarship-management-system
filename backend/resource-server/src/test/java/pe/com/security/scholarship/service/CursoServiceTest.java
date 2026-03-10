package pe.com.security.scholarship.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import pe.com.security.scholarship.domain.entity.Curso;
import pe.com.security.scholarship.domain.entity.Estudiante;
import pe.com.security.scholarship.domain.entity.HorarioSeccion;
import pe.com.security.scholarship.domain.entity.Postulacion;
import pe.com.security.scholarship.domain.entity.Seccion;
import pe.com.security.scholarship.domain.enums.DiaSemana;
import pe.com.security.scholarship.domain.enums.ModalidadCurso;
import pe.com.security.scholarship.dto.request.RegisterCursoRequest;
import pe.com.security.scholarship.dto.request.RegisterHorarioSeccionRequest;
import pe.com.security.scholarship.dto.request.RegisterSeccionRequest;
import pe.com.security.scholarship.dto.response.OverviewCursoResponse;
import pe.com.security.scholarship.dto.response.RegisteredCursoResponse;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.repository.CursoRepository;
import pe.com.security.scholarship.repository.EstudianteRepository;
import pe.com.security.scholarship.repository.PostulacionRepository;
import pe.com.security.scholarship.util.SecurityUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CursoServiceTest {

    @Mock
    private CursoRepository cursoRepository;

    @Mock
    private SeccionService seccionService;

    @Mock
    private EstudianteRepository estudianteRepository;

    @Mock
    private PostulacionRepository postulacionRepository;

    @Mock
    private PostulacionService postulacionService;

    @InjectMocks
    private CursoService cursoService;

    private static MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeAll
    static void setUpStatic() {
        securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
    }

    @AfterAll
    static void closeStatic() {
        securityUtilsMock.close();
    }

    @Test
    void register_ShouldSucceed_WhenCursoHasSecciones() {
        // Arrange
        RegisterCursoRequest request = new RegisterCursoRequest();
        request.setCodigo("CS101");
        request.setNombre("Introducción a la Programación");
        request.setModalidadCurso(ModalidadCurso.ONLINE);

        RegisterHorarioSeccionRequest h1 = new RegisterHorarioSeccionRequest();
        h1.setDiaSemana(DiaSemana.LUNES);
        h1.setHoraInicio(LocalTime.of(8, 0));
        h1.setHoraFin(LocalTime.of(10, 0));

        RegisterSeccionRequest s1 = new RegisterSeccionRequest();
        s1.setFechaInicio(LocalDate.now().plusDays(1));
        s1.setVacantesDisponibles(20);
        s1.setHorarios(Collections.singletonList(h1));

        RegisterSeccionRequest s2 = new RegisterSeccionRequest();
        s2.setFechaInicio(LocalDate.now().plusDays(2));
        s2.setVacantesDisponibles(25);
        s2.setHorarios(Collections.singletonList(h1));

        request.setSecciones(Arrays.asList(s1, s2));

        when(cursoRepository.existsByCodigo(request.getCodigo())).thenReturn(false);
        when(cursoRepository.save(any(Curso.class))).thenAnswer(invocation -> {
            Curso c = invocation.getArgument(0);
            c.setId(1);
            return c;
        });

        // Act
        RegisteredCursoResponse response = cursoService.register(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getSecciones()).hasSize(2);

        verify(seccionService, times(2)).horariosValidos(anyList());
        verify(cursoRepository).save(argThat(curso -> 
            curso.getSecciones() != null && 
            curso.getSecciones().size() == 2 &&
            !curso.getSecciones().getFirst().getHorarios().isEmpty()
        ));
    }

    @Test
    void register_ShouldSucceed_WhenCursoHasNoSecciones() {
        // Arrange
        RegisterCursoRequest request = new RegisterCursoRequest();
        request.setCodigo("CS102");
        request.setNombre("Algoritmos");
        request.setModalidadCurso(ModalidadCurso.PRESENCIAL);
        request.setSecciones(null);

        when(cursoRepository.existsByCodigo(request.getCodigo())).thenReturn(false);
        when(cursoRepository.save(any(Curso.class))).thenAnswer(invocation -> {
            Curso c = invocation.getArgument(0);
            c.setId(2);
            c.setSecciones(Collections.emptyList());
            return c;
        });

        // Act
        RegisteredCursoResponse response = cursoService.register(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2);
        assertThat(response.getSecciones()).isEmpty();

        verify(seccionService, never()).horariosValidos(anyList());
        verify(cursoRepository).save(any(Curso.class));
    }

    @Test
    void register_ShouldThrowBadRequest_WhenCodigoExists() {
        // Arrange
        RegisterCursoRequest request = new RegisterCursoRequest();
        request.setCodigo("CS101");
        request.setNombre("Curso Duplicado");
        request.setModalidadCurso(ModalidadCurso.ONLINE);

        when(cursoRepository.existsByCodigo(request.getCodigo())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> cursoService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Ya existe un curso con el código ingresado");

        verify(cursoRepository, never()).save(any());
        verify(seccionService, never()).horariosValidos(anyList());
    }

    @Test
    void register_ShouldPropagateException_WhenHorariosAreInvalid() {
        // Arrange
        RegisterCursoRequest request = new RegisterCursoRequest();
        request.setCodigo("CS103");
        request.setNombre("Curso Invalido");
        request.setModalidadCurso(ModalidadCurso.ONLINE);

        RegisterHorarioSeccionRequest h1 = new RegisterHorarioSeccionRequest();
        h1.setDiaSemana(DiaSemana.LUNES);
        h1.setHoraInicio(LocalTime.of(8, 0));
        h1.setHoraFin(LocalTime.of(10, 0));
        
        RegisterSeccionRequest s1 = new RegisterSeccionRequest();
        s1.setHorarios(Collections.singletonList(h1));
        
        request.setSecciones(Collections.singletonList(s1));

        when(cursoRepository.existsByCodigo(request.getCodigo())).thenReturn(false);
        
        doThrow(new BadRequestException("Cruce de horarios detectado"))
                .when(seccionService).horariosValidos(anyList());

        // Act & Assert
        assertThatThrownBy(() -> cursoService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cruce de horarios detectado");

        verify(cursoRepository, never()).save(any());
    }

    @Test
    void getCatalogo_ShouldReturnPageOfOverviewCursoResponse_WhenSortIsValid() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10, Sort.by("nombre"));
        Curso curso = Curso.builder()
                .id(1)
                .nombre("Curso Test")
                .codigo("C001")
                .modalidad(ModalidadCurso.ONLINE)
                .build();
        Page<Curso> page = new PageImpl<>(Collections.singletonList(curso));

        when(cursoRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<OverviewCursoResponse> result = cursoService.getCatalogo(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getNombre()).isEqualTo("Curso Test");
        verify(cursoRepository).findAll(pageable);
    }

    @Test
    void getCatalogo_ShouldThrowBadRequestException_WhenSortIsInvalid() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10, Sort.by("fechaCreacion"));

        // Act & Assert
        assertThatThrownBy(() -> cursoService.getCatalogo(pageable))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Campo de ordenamiento no permitido");

        verify(cursoRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getHorarios_ShouldReturnPageOfOverviewCursoResponse_WhenDataExists() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10, Sort.by("nombre"));
        LocalDate hoy = LocalDate.now();
        List<Integer> ids = Arrays.asList(1, 2);
        Page<Integer> idsPage = new PageImpl<>(ids, pageable, ids.size());

        HorarioSeccion horario = HorarioSeccion.builder()
                .diaSemana(DiaSemana.LUNES)
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(10, 0))
                .build();

        Seccion seccion = Seccion.builder()
                .id(10)
                .fechaInicio(hoy.plusDays(1))
                .horarios(Collections.singletonList(horario))
                .build();

        Curso curso1 = Curso.builder()
                .id(1)
                .nombre("Curso 1")
                .codigo("C001")
                .modalidad(ModalidadCurso.ONLINE)
                .secciones(Collections.singletonList(seccion))
                .build();
        
        Curso curso2 = Curso.builder()
                .id(2)
                .nombre("Curso 2")
                .codigo("C002")
                .modalidad(ModalidadCurso.PRESENCIAL)
                .secciones(Collections.emptyList())
                .build();

        when(cursoRepository.findIdsCursosHorarios(eq(hoy), eq(pageable))).thenReturn(idsPage);
        when(cursoRepository.findCursosSecciones(eq(ids), eq(hoy), eq(pageable.getSort())))
                .thenReturn(Arrays.asList(curso1, curso2));

        // Act
        Page<OverviewCursoResponse> result = cursoService.getHorarios(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().getFirst().getNombre()).isEqualTo("Curso 1");
        assertThat(result.getContent().getFirst().getSecciones()).hasSize(1);
        
        verify(cursoRepository).findIdsCursosHorarios(eq(hoy), eq(pageable));
        verify(cursoRepository).findCursosSecciones(eq(ids), eq(hoy), eq(pageable.getSort()));
    }

    @Test
    void getHorarios_ShouldReturnEmptyPage_WhenNoDataExists() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10, Sort.by("nombre"));
        LocalDate hoy = LocalDate.now();
        Page<Integer> idsPage = Page.empty(pageable);

        when(cursoRepository.findIdsCursosHorarios(eq(hoy), eq(pageable))).thenReturn(idsPage);

        // Act
        Page<OverviewCursoResponse> result = cursoService.getHorarios(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        
        verify(cursoRepository).findIdsCursosHorarios(eq(hoy), eq(pageable));
        verify(cursoRepository, never()).findCursosSecciones(anyList(), any(LocalDate.class), any(Sort.class));
    }

    @Test
    void getHorarios_ShouldThrowBadRequestException_WhenSortIsInvalid() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10, Sort.by("fechaCreacion"));

        // Act & Assert
        assertThatThrownBy(() -> cursoService.getHorarios(pageable))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Campo de ordenamiento no permitido");

        verify(cursoRepository, never()).findIdsCursosHorarios(any(LocalDate.class), any(Pageable.class));
    }

    @Test
    void getOfertaDisponiblePorBeca_DeberiaRetornarListaCursos_CuandoTodoEsValido() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        when(SecurityUtils.getCurrentUserId()).thenReturn(idUsuario);

        Estudiante estudiante = Estudiante.builder().id(UUID.randomUUID()).build();
        when(estudianteRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(estudiante));

        when(postulacionService.tieneBecaActiva(estudiante.getId())).thenReturn(true);

        Postulacion postulacion = Postulacion.builder().id(50).build();
        when(postulacionRepository.findLastPostulacion(estudiante.getId())).thenReturn(Optional.of(postulacion));

        Seccion seccion1 = Seccion.builder().id(100).fechaInicio(LocalDate.now().plusDays(5)).horarios(List.of()).build();
        Seccion seccion2 = Seccion.builder().id(101).fechaInicio(LocalDate.now().plusDays(2)).horarios(List.of()).build();
        
        Curso curso = Curso.builder()
                .id(1)
                .nombre("Curso Ofertado")
                .codigo("C100")
                .modalidad(ModalidadCurso.ONLINE)
                .secciones(Arrays.asList(seccion1, seccion2))
                .build();

        when(cursoRepository.findByIdPostulacion(eq(postulacion.getId()), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(curso));

        // Act
        List<OverviewCursoResponse> result = cursoService.getOfertaDisponiblePorBeca();

        // Assert
        assertThat(result).hasSize(1);
        OverviewCursoResponse cursoResponse = result.getFirst();
        assertThat(cursoResponse.getSecciones()).hasSize(2);
        // Verificar ordenamiento cronológico de secciones
        assertThat(cursoResponse.getSecciones().getFirst().getFechaInicio()).isBefore(cursoResponse.getSecciones().get(1).getFechaInicio());
    }

    @Test
    void getOfertaDisponiblePorBeca_DeberiaLanzarNotFound_CuandoEstudianteNoExiste() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        when(SecurityUtils.getCurrentUserId()).thenReturn(idUsuario);
        when(estudianteRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cursoService.getOfertaDisponiblePorBeca())
                .isInstanceOf(NotFoundException.class)
                .hasMessage("No se encontró estudiante asociado al id del payload");

        verify(postulacionService, never()).tieneBecaActiva(any());
    }

    @Test
    void getOfertaDisponiblePorBeca_DeberiaLanzarBadRequest_CuandoNoTieneBecaActiva() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        when(SecurityUtils.getCurrentUserId()).thenReturn(idUsuario);

        Estudiante estudiante = Estudiante.builder().id(UUID.randomUUID()).build();
        when(estudianteRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(estudiante));

        when(postulacionService.tieneBecaActiva(estudiante.getId())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> cursoService.getOfertaDisponiblePorBeca())
                .isInstanceOf(BadRequestException.class)
                .hasMessage("No tienes una beca vigente");
        
        verify(postulacionRepository, never()).findLastPostulacion(any());
    }

    @Test
    void getOfertaDisponiblePorBeca_DeberiaLanzarNotFound_CuandoNoExistePostulacion() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        when(SecurityUtils.getCurrentUserId()).thenReturn(idUsuario);

        Estudiante estudiante = Estudiante.builder().id(UUID.randomUUID()).build();
        when(estudianteRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(estudiante));

        when(postulacionService.tieneBecaActiva(estudiante.getId())).thenReturn(true);

        when(postulacionRepository.findLastPostulacion(estudiante.getId())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cursoService.getOfertaDisponiblePorBeca())
                .isInstanceOf(NotFoundException.class)
                .hasMessage("No se encontró postulación para el presente año");

        verify(cursoRepository, never()).findByIdPostulacion(any(), any());
    }
}
