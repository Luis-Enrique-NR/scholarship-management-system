package pe.com.security.scholarship.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
import pe.com.security.scholarship.dto.response.RegisteredCursoResponse;
import pe.com.security.scholarship.dto.response.RegisteredSeccionResponse;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.service.CursoService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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

        verify(cursoService, times(0)).register(any(RegisterCursoRequest.class));
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

        verify(cursoService, times(0)).register(any(RegisterCursoRequest.class));
    }
}
