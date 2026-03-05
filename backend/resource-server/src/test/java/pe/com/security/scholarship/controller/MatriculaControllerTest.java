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
import pe.com.security.scholarship.domain.enums.EstadoMatricula;
import pe.com.security.scholarship.dto.request.AprobarMatriculaRequest;
import pe.com.security.scholarship.dto.request.SubmitMatriculaRequest;
import pe.com.security.scholarship.dto.response.BecadoIntencionMatriculaResponse;
import pe.com.security.scholarship.dto.response.CursoIntencionMatriculaResponse;
import pe.com.security.scholarship.dto.response.IntencionMatriculaResponse;
import pe.com.security.scholarship.dto.response.RegisteredMatriculaResponse;
import pe.com.security.scholarship.dto.response.SeccionBecadosResponse;
import pe.com.security.scholarship.dto.response.SeccionIntencionMatriculaResponse;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.service.MatriculaService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @Test
    void getIntencionesMatriculaSeccion_ShouldReturnOk_WhenAuthorized() throws Exception {
        // Arrange
        SeccionIntencionMatriculaResponse seccionResponse = SeccionIntencionMatriculaResponse.builder()
                .idSeccion(101)
                .fechaInicio(LocalDate.now().plusDays(5))
                .totalIntencionesPendientes(10)
                .build();

        CursoIntencionMatriculaResponse cursoResponse = CursoIntencionMatriculaResponse.builder()
                .idCurso(1)
                .nombreCurso("Curso Test")
                .codigoCurso("C001")
                .secciones(List.of(seccionResponse))
                .build();

        when(matriculaService.getIntencionesMatriculaSeccion()).thenReturn(List.of(cursoResponse));

        // Act & Assert
        mockMvc.perform(get("/api/v1/matriculas/intenciones")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINING_CENTER_SECRETARY")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Consulta exitosa"))
                .andExpect(jsonPath("$.codigo").value("200"))
                .andExpect(jsonPath("$.data[0].idCurso").value(1))
                .andExpect(jsonPath("$.data[0].nombreCurso").value("Curso Test"))
                .andExpect(jsonPath("$.data[0].secciones[0].idSeccion").value(101))
                .andExpect(jsonPath("$.data[0].secciones[0].totalIntencionesPendientes").value(10));

        verify(matriculaService, times(1)).getIntencionesMatriculaSeccion();
    }

    @Test
    void getIntencionesMatriculaSeccion_ShouldReturnForbidden_WhenRoleIsInvalid() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/matriculas/intenciones")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(matriculaService, times(0)).getIntencionesMatriculaSeccion();
    }

    @Test
    void getBecadosIntencion_ShouldReturnOk_WhenAuthorized() throws Exception {
        // Arrange
        Integer idSeccion = 101;
        BecadoIntencionMatriculaResponse becadoResponse = BecadoIntencionMatriculaResponse.builder()
                .idMatricula(1)
                .nombreCompleto("Juan Perez")
                .codigo("S001")
                .promedioGeneral(18.5)
                .estadoMatricula(EstadoMatricula.PENDIENTE)
                .build();

        SeccionBecadosResponse response = SeccionBecadosResponse.builder()
                .idSeccion(idSeccion)
                .fechaInicio(LocalDate.now().plusDays(5))
                .vacantesTotales(20)
                .vacantesDisponibles(15)
                .becados(List.of(becadoResponse))
                .build();

        when(matriculaService.getBecadosSeccion(idSeccion)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/matriculas/intenciones/{idSeccion}", idSeccion)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINING_CENTER_SECRETARY")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Consulta exitosa"))
                .andExpect(jsonPath("$.codigo").value("200"))
                .andExpect(jsonPath("$.data.idSeccion").value(idSeccion))
                .andExpect(jsonPath("$.data.vacantesTotales").value(20))
                .andExpect(jsonPath("$.data.vacantesDisponibles").value(15))
                .andExpect(jsonPath("$.data.becados[0].idMatricula").value(1))
                .andExpect(jsonPath("$.data.becados[0].nombreCompleto").value("Juan Perez"))
                .andExpect(jsonPath("$.data.becados[0].estadoMatricula").value("PENDIENTE"));

        verify(matriculaService, times(1)).getBecadosSeccion(idSeccion);
    }

    @Test
    void getBecadosIntencion_ShouldReturnNotFound_WhenSectionDoesNotExist() throws Exception {
        // Arrange
        Integer idSeccion = 999;
        when(matriculaService.getBecadosSeccion(idSeccion))
                .thenThrow(new NotFoundException("No se encontró la sección con el ID enviado"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/matriculas/intenciones/{idSeccion}", idSeccion)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINING_CENTER_SECRETARY")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró la sección con el ID enviado"));

        verify(matriculaService, times(1)).getBecadosSeccion(idSeccion);
    }

    @Test
    void getBecadosIntencion_ShouldReturnForbidden_WhenRoleIsInvalid() throws Exception {
        // Arrange
        Integer idSeccion = 101;

        // Act & Assert
        mockMvc.perform(get("/api/v1/matriculas/intenciones/{idSeccion}", idSeccion)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(matriculaService, times(0)).getBecadosSeccion(idSeccion);
    }

    @Test
    void actualizarEstadoMatricula_ShouldReturnOk_WhenRequestIsValid() throws Exception {
        // Arrange
        AprobarMatriculaRequest request = new AprobarMatriculaRequest();
        request.setIdMatricula(1);
        request.setAprobado(true);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/matriculas")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINING_CENTER_SECRETARY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Actualización exitosa"))
                .andExpect(jsonPath("$.codigo").value("200"));

        verify(matriculaService, times(1)).actualizarEstadoMatricula(any(AprobarMatriculaRequest.class));
    }

    @Test
    void actualizarEstadoMatricula_ShouldReturnBadRequest_WhenServiceThrowsBadRequest() throws Exception {
        // Arrange
        AprobarMatriculaRequest request = new AprobarMatriculaRequest();
        request.setIdMatricula(1);
        request.setAprobado(true);

        doThrow(new BadRequestException("No se puede repetir el mismo estado"))
                .when(matriculaService).actualizarEstadoMatricula(any(AprobarMatriculaRequest.class));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/matriculas")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINING_CENTER_SECRETARY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No se puede repetir el mismo estado"));

        verify(matriculaService, times(1)).actualizarEstadoMatricula(any(AprobarMatriculaRequest.class));
    }

    @Test
    void actualizarEstadoMatricula_ShouldReturnNotFound_WhenServiceThrowsNotFound() throws Exception {
        // Arrange
        AprobarMatriculaRequest request = new AprobarMatriculaRequest();
        request.setIdMatricula(999);
        request.setAprobado(true);

        doThrow(new NotFoundException("No se encontró la matrícula con el ID enviado"))
                .when(matriculaService).actualizarEstadoMatricula(any(AprobarMatriculaRequest.class));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/matriculas")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINING_CENTER_SECRETARY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró la matrícula con el ID enviado"));

        verify(matriculaService, times(1)).actualizarEstadoMatricula(any(AprobarMatriculaRequest.class));
    }

    @Test
    void actualizarEstadoMatricula_ShouldReturnForbidden_WhenRoleIsInvalid() throws Exception {
        // Arrange
        AprobarMatriculaRequest request = new AprobarMatriculaRequest();
        request.setIdMatricula(1);
        request.setAprobado(true);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/matriculas")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(matriculaService, times(0)).actualizarEstadoMatricula(any(AprobarMatriculaRequest.class));
    }
}
