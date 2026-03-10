package pe.com.security.scholarship.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.security.scholarship.config.ResourceServerTest;
import pe.com.security.scholarship.domain.enums.DiaSemana;
import pe.com.security.scholarship.domain.enums.ModalidadCurso;
import pe.com.security.scholarship.dto.request.RegisterCursoRequest;
import pe.com.security.scholarship.dto.request.RegisterHorarioSeccionRequest;
import pe.com.security.scholarship.dto.request.RegisterSeccionRequest;
import pe.com.security.scholarship.dto.response.OverviewCursoResponse;
import pe.com.security.scholarship.dto.response.OverviewSeccionResponse;
import pe.com.security.scholarship.dto.response.RegisteredCursoResponse;
import pe.com.security.scholarship.dto.response.RegisteredSeccionResponse;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.service.CursoService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CursoController.class)
@ResourceServerTest
class CursoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CursoService cursoService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_ShouldReturnOk_WhenRequestIsValid() throws Exception {
        // Arrange
        RegisterCursoRequest request = new RegisterCursoRequest();
        request.setCodigo("CS101");
        request.setNombre("Introducción a la Programación");
        request.setModalidadCurso(ModalidadCurso.ONLINE);

        RegisterHorarioSeccionRequest horario = new RegisterHorarioSeccionRequest();
        horario.setDiaSemana(DiaSemana.LUNES);
        horario.setHoraInicio(LocalTime.of(8, 0));
        horario.setHoraFin(LocalTime.of(10, 0));

        RegisterSeccionRequest seccion = new RegisterSeccionRequest();
        seccion.setFechaInicio(LocalDate.now().plusDays(1));
        seccion.setVacantesDisponibles(20);
        seccion.setHorarios(Collections.singletonList(horario));

        request.setSecciones(Collections.singletonList(seccion));

        RegisteredSeccionResponse seccionResponse = RegisteredSeccionResponse.builder()
                .id(100)
                .vacantesDisponibles(20)
                .fechaInicio(seccion.getFechaInicio())
                .build();

        RegisteredCursoResponse response = RegisteredCursoResponse.builder()
                .id(1)
                .codigo(request.getCodigo())
                .nombre(request.getNombre())
                .modalidad(request.getModalidadCurso())
                .secciones(List.of(seccionResponse))
                .build();

        when(cursoService.register(any(RegisterCursoRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/cursos")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINING_CENTER_SECRETARY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registro exitoso"))
                .andExpect(jsonPath("$.codigo").value("200"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.codigo").value("CS101"))
                .andExpect(jsonPath("$.data.secciones[0].id").value(100));

        verify(cursoService, times(1)).register(any(RegisterCursoRequest.class));
    }

    @Test
    void register_ShouldReturnBadRequest_WhenCodigoExists() throws Exception {
        // Arrange
        RegisterCursoRequest request = new RegisterCursoRequest();
        request.setCodigo("CS101");
        request.setNombre("Curso Duplicado");
        request.setModalidadCurso(ModalidadCurso.ONLINE);

        when(cursoService.register(any(RegisterCursoRequest.class)))
                .thenThrow(new BadRequestException("Ya existe un curso con el código ingresado"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/cursos")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINING_CENTER_SECRETARY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ya existe un curso con el código ingresado"));

        verify(cursoService, times(1)).register(any(RegisterCursoRequest.class));
    }

    @Test
    void register_ShouldReturnBadRequest_WhenRequestIsInvalid() throws Exception {
        // Arrange
        RegisterCursoRequest request = new RegisterCursoRequest();
        request.setCodigo(null); // Inválido: nulo
        request.setNombre(""); // Inválido: vacío
        request.setModalidadCurso(null); // Inválido: nulo

        // Act & Assert
        mockMvc.perform(post("/api/v1/cursos")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINING_CENTER_SECRETARY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(cursoService, never()).register(any(RegisterCursoRequest.class));
    }

    @Test
    void register_ShouldReturnBadRequest_WhenHorariosAreInvalid() throws Exception {
        // Arrange
        RegisterCursoRequest request = new RegisterCursoRequest();
        request.setCodigo("CS102");
        request.setNombre("Curso con Horario Inválido");
        request.setModalidadCurso(ModalidadCurso.ONLINE);

        RegisterHorarioSeccionRequest horario = new RegisterHorarioSeccionRequest();
        horario.setDiaSemana(DiaSemana.LUNES);
        horario.setHoraInicio(LocalTime.of(10, 0));
        horario.setHoraFin(LocalTime.of(8, 0)); // Inválido: fin antes de inicio

        RegisterSeccionRequest seccion = new RegisterSeccionRequest();
        seccion.setFechaInicio(LocalDate.now().plusDays(1));
        seccion.setVacantesDisponibles(20);
        seccion.setHorarios(Collections.singletonList(horario));

        request.setSecciones(Collections.singletonList(seccion));

        // Simulamos que el servicio lanza la excepción de negocio al validar horarios
        when(cursoService.register(any(RegisterCursoRequest.class)))
                .thenThrow(new BadRequestException("Horario inconsistente"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/cursos")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINING_CENTER_SECRETARY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Horario inconsistente"));

        verify(cursoService, times(1)).register(any(RegisterCursoRequest.class));
    }

    @Test
    void register_ShouldReturnForbidden_WhenRoleIsUnauthorized() throws Exception {
        // Arrange
        RegisterCursoRequest request = new RegisterCursoRequest();
        request.setCodigo("CS103");
        request.setNombre("Curso Prohibido");
        request.setModalidadCurso(ModalidadCurso.ONLINE);

        // Act & Assert
        mockMvc.perform(post("/api/v1/cursos")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(cursoService, never()).register(any(RegisterCursoRequest.class));
    }

    @Test
    void getCatalogo_ShouldReturnOk_WhenDefaultParameters() throws Exception {
        // Arrange
        OverviewCursoResponse curso = OverviewCursoResponse.builder()
                .id(1)
                .nombre("Curso Test")
                .codigo("C001")
                .modalidad(ModalidadCurso.ONLINE)
                .build();
        PageImpl<OverviewCursoResponse> page = new PageImpl<>(Collections.singletonList(curso));

        when(cursoService.getCatalogo(any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/cursos")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Consulta exitosa"))
                .andExpect(jsonPath("$.codigo").value("200"))
                .andExpect(jsonPath("$.data.content[0].nombre").value("Curso Test"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(cursoService).getCatalogo(eq(PageRequest.of(0, 10, Sort.by("nombre").ascending())));
    }

    @Test
    void getCatalogo_ShouldReturnOk_WhenCustomParameters() throws Exception {
        // Arrange
        OverviewCursoResponse curso = OverviewCursoResponse.builder()
                .id(1)
                .nombre("Curso Test")
                .codigo("C001")
                .modalidad(ModalidadCurso.ONLINE)
                .build();
        PageImpl<OverviewCursoResponse> page = new PageImpl<>(Collections.singletonList(curso));

        when(cursoService.getCatalogo(any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/cursos")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "modalidad,desc")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Consulta exitosa"))
                .andExpect(jsonPath("$.codigo").value("200"))
                .andExpect(jsonPath("$.data.content[0].nombre").value("Curso Test"));

        verify(cursoService).getCatalogo(eq(PageRequest.of(1, 5, Sort.by("modalidad").descending())));
    }

    @Test
    void getCatalogo_ShouldReturnBadRequest_WhenSortIsInvalid() throws Exception {
        // Arrange
        when(cursoService.getCatalogo(any(Pageable.class)))
                .thenThrow(new BadRequestException("Campo de ordenamiento no permitido: fechaCreacion"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/cursos")
                        .param("sort", "fechaCreacion,asc")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Campo de ordenamiento no permitido: fechaCreacion"));
    }

    @Test
    void getHorarios_ShouldReturnOk_WhenDefaultParameters() throws Exception {
        // Arrange
        OverviewCursoResponse curso = OverviewCursoResponse.builder()
                .id(1)
                .nombre("Curso Horario")
                .codigo("C002")
                .modalidad(ModalidadCurso.PRESENCIAL)
                .build();
        PageImpl<OverviewCursoResponse> page = new PageImpl<>(Collections.singletonList(curso));

        when(cursoService.getHorarios(any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/cursos/horarios")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Consulta exitosa"))
                .andExpect(jsonPath("$.codigo").value("200"))
                .andExpect(jsonPath("$.data.content[0].nombre").value("Curso Horario"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(cursoService).getHorarios(eq(PageRequest.of(0, 10, Sort.by("nombre").ascending())));
    }

    @Test
    void getHorarios_ShouldReturnOk_WhenCustomParameters() throws Exception {
        // Arrange
        OverviewCursoResponse curso = OverviewCursoResponse.builder()
                .id(1)
                .nombre("Curso Horario")
                .codigo("C002")
                .modalidad(ModalidadCurso.PRESENCIAL)
                .build();
        PageImpl<OverviewCursoResponse> page = new PageImpl<>(Collections.singletonList(curso));

        when(cursoService.getHorarios(any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/cursos/horarios")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "modalidad,desc")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Consulta exitosa"))
                .andExpect(jsonPath("$.codigo").value("200"))
                .andExpect(jsonPath("$.data.content[0].nombre").value("Curso Horario"));

        verify(cursoService).getHorarios(eq(PageRequest.of(1, 5, Sort.by("modalidad").descending())));
    }

    @Test
    void getHorarios_ShouldReturnBadRequest_WhenSortIsInvalid() throws Exception {
        // Arrange
        when(cursoService.getHorarios(any(Pageable.class)))
                .thenThrow(new BadRequestException("Campo de ordenamiento no permitido: fechaCreacion"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/cursos/horarios")
                        .param("sort", "fechaCreacion,asc")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Campo de ordenamiento no permitido: fechaCreacion"));
    }

    @Test
    void getCursosDisponibles_ShouldReturnOk_WhenRoleIsStudent() throws Exception {
        // Arrange
        OverviewSeccionResponse seccion = OverviewSeccionResponse.builder().id(10).build();
        OverviewCursoResponse curso = OverviewCursoResponse.builder()
                .id(1)
                .nombre("Curso para Beca")
                .secciones(List.of(seccion))
                .build();
        when(cursoService.getOfertaDisponiblePorBeca()).thenReturn(List.of(curso));

        // Act & Assert
        mockMvc.perform(get("/api/v1/cursos/beca")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Consulta exitosa"))
                .andExpect(jsonPath("$.codigo").value("200"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].nombre").value("Curso para Beca"));

        verify(cursoService, times(1)).getOfertaDisponiblePorBeca();
    }

    @Test
    void getCursosDisponibles_ShouldReturnForbidden_WhenRoleIsIncorrect() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/cursos/beca")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(cursoService, never()).getOfertaDisponiblePorBeca();
    }

    @Test
    void getCursosDisponibles_ShouldReturnUnauthorized_WhenAnonymous() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/cursos/beca")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(cursoService, never()).getOfertaDisponiblePorBeca();
    }

    @Test
    void getCursosDisponibles_ShouldReturnBadRequest_WhenServiceThrowsBadRequest() throws Exception {
        // Arrange
        when(cursoService.getOfertaDisponiblePorBeca())
                .thenThrow(new BadRequestException("No tienes una beca vigente"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/cursos/beca")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No tienes una beca vigente"));
    }

    @Test
    void getCursosDisponibles_ShouldReturnNotFound_WhenServiceThrowsNotFound() throws Exception {
        // Arrange
        when(cursoService.getOfertaDisponiblePorBeca())
                .thenThrow(new NotFoundException("No se encontró postulación"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/cursos/beca")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró postulación"));
    }
}
