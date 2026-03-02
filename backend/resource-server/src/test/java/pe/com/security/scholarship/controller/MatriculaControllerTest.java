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
import pe.com.security.scholarship.dto.request.SubmitMatriculaRequest;
import pe.com.security.scholarship.dto.response.IntencionMatriculaResponse;
import pe.com.security.scholarship.dto.response.RegisteredMatriculaResponse;
import pe.com.security.scholarship.service.MatriculaService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MatriculaController.class)
@ResourceServerTest
class MatriculaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatriculaService matriculaService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void submitEnrollmentIntention_ShouldReturnOk_WhenRequestIsValid() throws Exception {
        // Arrange
        SubmitMatriculaRequest request = new SubmitMatriculaRequest();
        request.setIdSeccion(1);

        RegisteredMatriculaResponse response = RegisteredMatriculaResponse.builder()
                .idMatricula(1)
                .nombreCurso("Java Basics")
                .build();

        when(matriculaService.submitEnrollmentIntention(any(SubmitMatriculaRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/matriculas")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registro exitoso"))
                .andExpect(jsonPath("$.codigo").value("200"))
                .andExpect(jsonPath("$.data.idMatricula").value(1))
                .andExpect(jsonPath("$.data.nombreCurso").value("Java Basics"));

        verify(matriculaService, times(1)).submitEnrollmentIntention(any(SubmitMatriculaRequest.class));
    }

    @Test
    void submitEnrollmentIntention_ShouldReturnBadRequest_WhenRequestIsInvalid() throws Exception {
        // Arrange
        SubmitMatriculaRequest request = new SubmitMatriculaRequest();
        // Missing required field idSeccion

        // Act & Assert
        mockMvc.perform(post("/api/v1/matriculas")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(matriculaService, times(0)).submitEnrollmentIntention(any(SubmitMatriculaRequest.class));
    }

    @Test
    void submitEnrollmentIntention_ShouldReturnForbidden_WhenRoleIsInvalid() throws Exception {
        // Arrange
        SubmitMatriculaRequest request = new SubmitMatriculaRequest();
        request.setIdSeccion(1);

        // Act & Assert
        mockMvc.perform(post("/api/v1/matriculas")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(matriculaService, times(0)).submitEnrollmentIntention(any(SubmitMatriculaRequest.class));
    }

    @Test
    void getIntencionMatricula_ShouldReturnOk_WhenAuthorized() throws Exception {
        // Arrange
        IntencionMatriculaResponse response = IntencionMatriculaResponse.builder()
                .idMatricula(1)
                .nombreCurso("Java Basics")
                .build();

        when(matriculaService.getIntencionMatricula()).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/matriculas")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registro exitoso"))
                .andExpect(jsonPath("$.codigo").value("200"))
                .andExpect(jsonPath("$.data.idMatricula").value(1))
                .andExpect(jsonPath("$.data.nombreCurso").value("Java Basics"));

        verify(matriculaService, times(1)).getIntencionMatricula();
    }

    @Test
    void getIntencionMatricula_ShouldReturnForbidden_WhenRoleIsInvalid() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/matriculas")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(matriculaService, times(0)).getIntencionMatricula();
    }

    @Test
    void getIntencionMatricula_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/matriculas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(matriculaService, times(0)).getIntencionMatricula();
    }
}
