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
import pe.com.security.scholarship.dto.request.RegisterHorarioSeccionRequest;
import pe.com.security.scholarship.dto.request.RegisterSeccionRequest;
import pe.com.security.scholarship.dto.request.UpdateVacantesSeccionRequest;
import pe.com.security.scholarship.dto.response.RegisteredSeccionResponse;
import pe.com.security.scholarship.dto.response.UpdatedVacantesSeccionResponse;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.service.SeccionService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeccionController.class)
@ResourceServerTest
class SeccionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SeccionService seccionService;

    @Autowired
    private ObjectMapper objectMapper;

    // --- Tests para register (POST) ---

    @Test
    void register_ShouldReturnOk_WhenRequestIsValid() throws Exception {
        // Arrange
        Integer idCurso = 1;
        RegisterSeccionRequest request = new RegisterSeccionRequest();
        request.setFechaInicio(LocalDate.now().plusDays(1));
        request.setVacantesDisponibles(20);

        RegisterHorarioSeccionRequest horario = new RegisterHorarioSeccionRequest();
        horario.setDiaSemana(DiaSemana.LUNES);
        horario.setHoraInicio(LocalTime.of(8, 0));
        horario.setHoraFin(LocalTime.of(10, 0));
        request.setHorarios(Collections.singletonList(horario));

        RegisteredSeccionResponse response = RegisteredSeccionResponse.builder()
                .id(100)
                .vacantesDisponibles(20)
                .fechaInicio(request.getFechaInicio())
                .build();

        when(seccionService.register(any(RegisterSeccionRequest.class), eq(idCurso))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/secciones/{idCurso}", idCurso)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINING_CENTER_SECRETARY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registro exitoso"))
                .andExpect(jsonPath("$.codigo").value("200"))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.vacantesDisponibles").value(20));

        verify(seccionService, times(1)).register(any(RegisterSeccionRequest.class), eq(idCurso));
    }

    @Test
    void register_ShouldReturnBadRequest_WhenRequestIsInvalid() throws Exception {
        // Arrange
        Integer idCurso = 1;
        RegisterSeccionRequest request = new RegisterSeccionRequest();
        request.setFechaInicio(LocalDate.now().plusDays(1));
        request.setVacantesDisponibles(-5); // Inválido: negativo
        request.setHorarios(Collections.emptyList()); // Inválido: vacío

        // Act & Assert
        mockMvc.perform(post("/api/v1/secciones/{idCurso}", idCurso)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINING_CENTER_SECRETARY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // No se debe llamar al servicio si falla la validación del DTO
        verify(seccionService, times(0)).register(any(RegisterSeccionRequest.class), any());
    }

    @Test
    void register_ShouldReturnNotFound_WhenCursoDoesNotExist() throws Exception {
        // Arrange
        Integer idCurso = 999;
        RegisterSeccionRequest request = new RegisterSeccionRequest();
        request.setFechaInicio(LocalDate.now().plusDays(1));
        request.setVacantesDisponibles(20);

        RegisterHorarioSeccionRequest horario = new RegisterHorarioSeccionRequest();
        horario.setDiaSemana(DiaSemana.LUNES);
        horario.setHoraInicio(LocalTime.of(8, 0));
        horario.setHoraFin(LocalTime.of(10, 0));
        request.setHorarios(Collections.singletonList(horario));

        when(seccionService.register(any(RegisterSeccionRequest.class), eq(idCurso)))
                .thenThrow(new NotFoundException("No se encontró curso con el ID ingresado"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/secciones/{idCurso}", idCurso)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINING_CENTER_SECRETARY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró curso con el ID ingresado"));

        verify(seccionService, times(1)).register(any(RegisterSeccionRequest.class), eq(idCurso));
    }

    @Test
    void register_ShouldReturnBadRequest_WhenHorariosAreInvalid() throws Exception {
        // Arrange
        Integer idCurso = 1;
        RegisterSeccionRequest request = new RegisterSeccionRequest();
        request.setFechaInicio(LocalDate.now().plusDays(1));
        request.setVacantesDisponibles(20);

        RegisterHorarioSeccionRequest horario = new RegisterHorarioSeccionRequest();
        horario.setDiaSemana(DiaSemana.LUNES);
        horario.setHoraInicio(LocalTime.of(10, 0));
        horario.setHoraFin(LocalTime.of(8, 0)); // Inválido lógica negocio
        request.setHorarios(Collections.singletonList(horario));

        when(seccionService.register(any(RegisterSeccionRequest.class), eq(idCurso)))
                .thenThrow(new BadRequestException("Horario inconsistente"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/secciones/{idCurso}", idCurso)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINING_CENTER_SECRETARY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Horario inconsistente"));

        verify(seccionService, times(1)).register(any(RegisterSeccionRequest.class), eq(idCurso));
    }

    @Test
    void register_ShouldReturnForbidden_WhenRoleIsUnauthorized() throws Exception {
        // Arrange
        Integer idCurso = 1;
        RegisterSeccionRequest request = new RegisterSeccionRequest();
        request.setFechaInicio(LocalDate.now().plusDays(1));
        request.setVacantesDisponibles(20);

        RegisterHorarioSeccionRequest horario = new RegisterHorarioSeccionRequest();
        horario.setDiaSemana(DiaSemana.LUNES);
        horario.setHoraInicio(LocalTime.of(8, 0));
        horario.setHoraFin(LocalTime.of(10, 0));
        request.setHorarios(Collections.singletonList(horario));

        // Act & Assert
        mockMvc.perform(post("/api/v1/secciones/{idCurso}", idCurso)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(seccionService, times(0)).register(any(RegisterSeccionRequest.class), any());
    }

    // --- Tests para updateVacantes (existentes) ---

    @Test
    void updateVacantesSeccion_ShouldReturnOk_WhenRequestIsValid() throws Exception {
        // Arrange
        UpdateVacantesSeccionRequest request = new UpdateVacantesSeccionRequest();
        request.setIdSeccion(1);
        request.setCantidadVacantes(20);

        UpdatedVacantesSeccionResponse response = UpdatedVacantesSeccionResponse.builder()
                .cantidadNuevosMatriculados(5)
                .totalMatriculados(20)
                .build();

        when(seccionService.updateVacantes(any(UpdateVacantesSeccionRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/secciones/vacantes")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINING_CENTER_SECRETARY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Actualización exitosa"))
                .andExpect(jsonPath("$.codigo").value("200"))
                .andExpect(jsonPath("$.data.cantidadNuevosMatriculados").value(5))
                .andExpect(jsonPath("$.data.totalMatriculados").value(20));

        verify(seccionService, times(1)).updateVacantes(any(UpdateVacantesSeccionRequest.class));
    }

    @Test
    void updateVacantesSeccion_ShouldReturnBadRequest_WhenServiceThrowsBadRequest() throws Exception {
        // Arrange
        UpdateVacantesSeccionRequest request = new UpdateVacantesSeccionRequest();
        request.setIdSeccion(1);
        request.setCantidadVacantes(10); // Menor o igual a las actuales

        when(seccionService.updateVacantes(any(UpdateVacantesSeccionRequest.class)))
                .thenThrow(new BadRequestException("La nueva cantidad de vacantes debe superar a las disponibles actualmente"));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/secciones/vacantes")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINING_CENTER_SECRETARY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La nueva cantidad de vacantes debe superar a las disponibles actualmente"));

        verify(seccionService, times(1)).updateVacantes(any(UpdateVacantesSeccionRequest.class));
    }

    @Test
    void updateVacantesSeccion_ShouldReturnNotFound_WhenServiceThrowsNotFound() throws Exception {
        // Arrange
        UpdateVacantesSeccionRequest request = new UpdateVacantesSeccionRequest();
        request.setIdSeccion(999);
        request.setCantidadVacantes(20);

        when(seccionService.updateVacantes(any(UpdateVacantesSeccionRequest.class)))
                .thenThrow(new NotFoundException("No se encontró la sección con el ID ingresado"));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/secciones/vacantes")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINING_CENTER_SECRETARY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró la sección con el ID ingresado"));

        verify(seccionService, times(1)).updateVacantes(any(UpdateVacantesSeccionRequest.class));
    }

    @Test
    void updateVacantesSeccion_ShouldReturnForbidden_WhenRoleIsInvalid() throws Exception {
        // Arrange
        UpdateVacantesSeccionRequest request = new UpdateVacantesSeccionRequest();
        request.setIdSeccion(1);
        request.setCantidadVacantes(20);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/secciones/vacantes")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(seccionService, times(0)).updateVacantes(any(UpdateVacantesSeccionRequest.class));
    }
}
