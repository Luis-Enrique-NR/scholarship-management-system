package pe.com.security.scholarship.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.security.scholarship.config.ResourceServerTest;
import pe.com.security.scholarship.dto.request.RegisterConvocatoriaRequest;
import pe.com.security.scholarship.dto.response.RegisteredConvocatoriaResponse;
import pe.com.security.scholarship.entity.enums.Mes;
import pe.com.security.scholarship.service.ConvocatoriaService;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConvocatoriaController.class)
@ResourceServerTest
class ConvocatoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConvocatoriaService convocatoriaService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "user", roles = {"SOCIAL_OUTREACH_SECRETARY"})
    void registerConvocatoria_ShouldReturnOk_WhenRequestIsValid() throws Exception {
        // Arrange
        RegisterConvocatoriaRequest request = new RegisterConvocatoriaRequest();
        request.setMes(Mes.ENERO);
        request.setFechaInicio(LocalDate.now());
        request.setFechaFin(LocalDate.now().plusDays(30));
        request.setCantidadVacantes(10);

        RegisteredConvocatoriaResponse response = RegisteredConvocatoriaResponse.builder()
                .mes(Mes.ENERO)
                .cantidadVacantes(10)
                .build();

        when(convocatoriaService.registerConvocatoria(any(RegisterConvocatoriaRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/convocatorias")
                        .with(csrf()) // Importante para POST/PUT/DELETE
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mes").value("ENERO"))
                .andExpect(jsonPath("$.data.cantidadVacantes").value(10));

        verify(convocatoriaService, times(1)).registerConvocatoria(any(RegisterConvocatoriaRequest.class));
    }

    @Test
    @WithMockUser(username = "user", roles = {"SOCIAL_OUTREACH_SECRETARY"})
    void registerConvocatoria_ShouldReturnBadRequest_WhenRequestIsInvalid() throws Exception {
        // Arrange
        RegisterConvocatoriaRequest request = new RegisterConvocatoriaRequest();
        // Missing required fields to trigger validation error

        // Act & Assert
        mockMvc.perform(post("/api/v1/convocatorias")
                        .with(csrf()) // Importante para POST/PUT/DELETE
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(convocatoriaService, times(0)).registerConvocatoria(any(RegisterConvocatoriaRequest.class));
    }
}
