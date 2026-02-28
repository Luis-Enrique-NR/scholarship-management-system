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
import pe.com.security.scholarship.dto.request.RegisterPostulacionRequest;
import pe.com.security.scholarship.dto.response.ConsultaPostulacionResponse;
import pe.com.security.scholarship.dto.response.DetallePostulanteResponse;
import pe.com.security.scholarship.dto.response.HistorialPostulacionResponse;
import pe.com.security.scholarship.dto.response.InformacionMatriculaResponse;
import pe.com.security.scholarship.dto.response.InformacionPostulacionResponse;
import pe.com.security.scholarship.dto.response.RegisteredPostulacionResponse;
import pe.com.security.scholarship.dto.response.ResultadoPostulacionResponse;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.service.PostulacionService;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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

@WebMvcTest(PostulacionController.class)
@ResourceServerTest
class PostulacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostulacionService postulacionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerPostulacion_ShouldReturnOk_WhenRequestIsValid() throws Exception {
        // Arrange
        RegisterPostulacionRequest request = new RegisterPostulacionRequest();
        request.setIdConvocatoria(1);
        request.setIdsCursos(List.of(1, 2));

        RegisteredPostulacionResponse response = RegisteredPostulacionResponse.builder()
                .id(1)
                .build();

        when(postulacionService.registerPostulacion(any(RegisterPostulacionRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/postulaciones")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registro exitoso"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(postulacionService, times(1)).registerPostulacion(any(RegisterPostulacionRequest.class));
    }

    @Test
    void registerPostulacion_ShouldReturnForbidden_WhenRoleIsInvalid() throws Exception {
        // Arrange
        RegisterPostulacionRequest request = new RegisterPostulacionRequest();
        request.setIdConvocatoria(1);
        request.setIdsCursos(List.of(1, 2));

        // Act & Assert
        mockMvc.perform(post("/api/v1/postulaciones")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOCIAL_OUTREACH_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(postulacionService, times(0)).registerPostulacion(any(RegisterPostulacionRequest.class));
    }

    @Test
    void registerPostulacion_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
        // Arrange
        RegisterPostulacionRequest request = new RegisterPostulacionRequest();
        request.setIdConvocatoria(1);
        request.setIdsCursos(List.of(1, 2));

        // Act & Assert
        mockMvc.perform(post("/api/v1/postulaciones")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(postulacionService, times(0)).registerPostulacion(any(RegisterPostulacionRequest.class));
    }

    @Test
    void registerPostulacion_ShouldReturnBadRequest_WhenRequestIsInvalid() throws Exception {
        // Arrange
        RegisterPostulacionRequest request = new RegisterPostulacionRequest();
        // Missing required fields

        // Act & Assert
        mockMvc.perform(post("/api/v1/postulaciones")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(postulacionService, times(0)).registerPostulacion(any(RegisterPostulacionRequest.class));
    }

    @Test
    void getDetallePostulacion_ShouldReturnOk_WhenAuthorized() throws Exception {
        // Arrange
        Integer idPostulacion = 1;
        ConsultaPostulacionResponse response = ConsultaPostulacionResponse.builder()
                .id(idPostulacion)
                .build();

        when(postulacionService.getDetallePostulacion(idPostulacion)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/postulaciones/{idPostulacion}", idPostulacion)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Consulta exitosa"))
                .andExpect(jsonPath("$.data.id").value(idPostulacion));

        verify(postulacionService, times(1)).getDetallePostulacion(idPostulacion);
    }

    @Test
    void getHistorialPostulacion_ShouldReturnOk_WhenAuthorized() throws Exception {
        // Arrange
        Integer year = 2023;
        HistorialPostulacionResponse historial = HistorialPostulacionResponse.builder()
                .id(1)
                .build();
        List<HistorialPostulacionResponse> response = List.of(historial);

        when(postulacionService.getHistorialPostulacion(year)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/postulaciones/historial")
                        .param("year", year.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Consulta exitosa"))
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(postulacionService, times(1)).getHistorialPostulacion(year);
    }

    @Test
    void getDetallePostulacion_ShouldReturnForbidden_WhenRoleIsInvalid() throws Exception {
        // Arrange
        Integer idPostulacion = 1;

        // Act & Assert
        mockMvc.perform(get("/api/v1/postulaciones/{idPostulacion}", idPostulacion)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOCIAL_OUTREACH_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(postulacionService, times(0)).getDetallePostulacion(idPostulacion);
    }

    @Test
    void getHistorialPostulacion_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
        // Arrange
        Integer year = 2023;

        // Act & Assert
        mockMvc.perform(get("/api/v1/postulaciones/historial")
                        .param("year", year.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(postulacionService, times(0)).getHistorialPostulacion(year);
    }

    @Test
    void getPostulacionesEstudiante_ShouldReturnOk_WhenAuthorizedAsManager() throws Exception {
        // Arrange
        UUID idEstudiante = UUID.randomUUID();
        Integer year = 2023;
        String nombreCompleto = "Juan Perez";

        InformacionPostulacionResponse infoPostulacion = InformacionPostulacionResponse.builder()
                .mesConvocatoria("ENERO")
                .estadoPostulacion("Aceptado")
                .fechaPostulacion(LocalDate.now())
                .promedioGeneral(15.0)
                .cursosOpciones(List.of("Java"))
                .build();

        InformacionMatriculaResponse infoMatricula = InformacionMatriculaResponse.builder()
                .fechaMatricula(LocalDate.now())
                .cursoMatriculado("Java")
                .notaMatricula(18.0)
                .build();

        ResultadoPostulacionResponse resultado = ResultadoPostulacionResponse.builder()
                .postulacion(infoPostulacion)
                .matricula(infoMatricula)
                .build();

        DetallePostulanteResponse response = DetallePostulanteResponse.builder()
                .idEstudiante(idEstudiante)
                .nombreCompleto(nombreCompleto)
                .codigoEstudiante("C12345")
                .postulaciones(List.of(resultado))
                .build();

        when(postulacionService.getPostulacionesEstudiante(idEstudiante, year)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/postulaciones/{id}/{year}", idEstudiante, year)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOCIAL_OUTREACH_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Consulta exitosa"))
                .andExpect(jsonPath("$.codigo").value("200"))
                .andExpect(jsonPath("$.data.nombreCompleto").value(nombreCompleto))
                .andExpect(jsonPath("$.data.postulaciones").isArray())
                .andExpect(jsonPath("$.data.postulaciones").isNotEmpty());

        verify(postulacionService, times(1)).getPostulacionesEstudiante(idEstudiante, year);
    }

    @Test
    void getPostulacionesEstudiante_ShouldReturnOk_WhenAuthorizedAsSecretary() throws Exception {
        // Arrange
        UUID idEstudiante = UUID.randomUUID();
        Integer year = 2023;
        DetallePostulanteResponse response = DetallePostulanteResponse.builder()
                .idEstudiante(idEstudiante)
                .postulaciones(Collections.emptyList())
                .build();

        when(postulacionService.getPostulacionesEstudiante(idEstudiante, year)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/postulaciones/{id}/{year}", idEstudiante, year)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOCIAL_OUTREACH_SECRETARY")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Consulta exitosa"));

        verify(postulacionService, times(1)).getPostulacionesEstudiante(idEstudiante, year);
    }

    @Test
    void getPostulacionesEstudiante_ShouldReturnForbidden_WhenRoleIsUnauthorized() throws Exception {
        // Arrange
        UUID idEstudiante = UUID.randomUUID();
        Integer year = 2023;

        // Act & Assert
        mockMvc.perform(get("/api/v1/postulaciones/{id}/{year}", idEstudiante, year)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(postulacionService, times(0)).getPostulacionesEstudiante(any(), any());
    }

    @Test
    void getPostulacionesEstudiante_ShouldReturnNotFound_WhenStudentDoesNotExist() throws Exception {
        // Arrange
        UUID idEstudiante = UUID.randomUUID();
        Integer year = 2023;

        when(postulacionService.getPostulacionesEstudiante(idEstudiante, year))
                .thenThrow(new NotFoundException("No existe estudiante con el ID ingresado"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/postulaciones/{id}/{year}", idEstudiante, year)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOCIAL_OUTREACH_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
                // Asumiendo que existe un GlobalExceptionHandler que maneja NotFoundException
                // y retorna un 404. Si no, esto fallará o retornará 500.
                // En un entorno real de WebMvcTest, el ExceptionHandler debería estar cargado.

        verify(postulacionService, times(1)).getPostulacionesEstudiante(idEstudiante, year);
    }

    @Test
    void getPostulacionesEstudiante_ShouldReturnBadRequest_WhenParametersAreInvalid() throws Exception {
        // Arrange
        String invalidUuid = "invalid-uuid";
        String invalidYear = "not-a-number";

        // Act & Assert - Invalid UUID
        mockMvc.perform(get("/api/v1/postulaciones/{id}/{year}", invalidUuid, 2023)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOCIAL_OUTREACH_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Act & Assert - Invalid Year
        mockMvc.perform(get("/api/v1/postulaciones/{id}/{year}", UUID.randomUUID(), invalidYear)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOCIAL_OUTREACH_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(postulacionService, times(0)).getPostulacionesEstudiante(any(), any());
    }
}
