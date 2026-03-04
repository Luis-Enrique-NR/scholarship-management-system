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
import pe.com.security.scholarship.dto.request.UpdateVacantesSeccionRequest;
import pe.com.security.scholarship.dto.response.UpdatedVacantesSeccionResponse;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.service.SeccionService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
