package pe.com.security.scholarship.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.autoconfigure.web.DataWebAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.security.scholarship.config.ResourceServerTest;
import pe.com.security.scholarship.domain.enums.EstadoConvocatoria;
import pe.com.security.scholarship.domain.enums.Mes;
import pe.com.security.scholarship.domain.enums.ModoEvaluacion;
import pe.com.security.scholarship.dto.request.RegisterConvocatoriaRequest;
import pe.com.security.scholarship.dto.request.UpdateEstadoConvocatoriaRequest;
import pe.com.security.scholarship.dto.response.ConvocatoriaAbiertaResponse;
import pe.com.security.scholarship.dto.response.DetalleConvocatoriaResponse;
import pe.com.security.scholarship.dto.response.HistorialConvocatoriaResponse;
import pe.com.security.scholarship.dto.response.PostulanteConvocatoriaResponse;
import pe.com.security.scholarship.dto.response.RegisteredConvocatoriaResponse;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.service.ConvocatoriaService;
import pe.com.security.scholarship.service.PostulacionService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

@WebMvcTest(ConvocatoriaController.class)
@ResourceServerTest
@Import(DataWebAutoConfiguration.class)
class ConvocatoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConvocatoriaService convocatoriaService;

    @MockitoBean
    private PostulacionService postulacionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerConvocatoria_ShouldReturnOk_WhenRequestIsValid() throws Exception {
        // Arrange
        RegisterConvocatoriaRequest request = new RegisterConvocatoriaRequest();
        request.setMes(Mes.ENERO);
        request.setFechaInicio(LocalDate.now());
        request.setFechaFin(LocalDate.now().plusDays(30));
        request.setCantidadVacantes(10);
        request.setModoEvaluacion(ModoEvaluacion.MIXTO);

        RegisteredConvocatoriaResponse response = RegisteredConvocatoriaResponse.builder()
                .mes(Mes.ENERO)
                .cantidadVacantes(10)
                .modoEvaluacion(ModoEvaluacion.MIXTO)
                .build();

        when(convocatoriaService.registerConvocatoria(any(RegisterConvocatoriaRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/convocatorias")
                        .with(csrf())
                        // Usamos ROLE_SOCIAL_OUTREACH_MANAGER para escritura como solicitado
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOCIAL_OUTREACH_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mes").value("ENERO"))
                .andExpect(jsonPath("$.data.cantidadVacantes").value(10))
                .andExpect(jsonPath("$.data.modoEvaluacion").value("MIXTO"));

        verify(convocatoriaService, times(1)).registerConvocatoria(any(RegisterConvocatoriaRequest.class));
    }

    @Test
    void registerConvocatoria_ShouldReturnBadRequest_WhenRequestIsInvalid() throws Exception {
        // Arrange
        RegisterConvocatoriaRequest request = new RegisterConvocatoriaRequest();
        // Missing required fields to trigger validation error

        // Act & Assert
        mockMvc.perform(post("/api/v1/convocatorias")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOCIAL_OUTREACH_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(convocatoriaService, times(0)).registerConvocatoria(any(RegisterConvocatoriaRequest.class));
    }

    @Test
    void getDetallesConvocatoria_ShouldReturnOk_WhenAuthorized() throws Exception {
        // Arrange
        Integer id = 1;
        
        HistorialConvocatoriaResponse datosGenerales = HistorialConvocatoriaResponse.builder()
                .mes(Mes.ENERO)
                .build();

        DetalleConvocatoriaResponse response = DetalleConvocatoriaResponse.builder()
                .datosGeneralesConvocatoria(datosGenerales)
                .cantidadPostulantes(100)
                .tasaVacantesCubiertas("80%") // Nuevo campo
                .modoEvaluacion(ModoEvaluacion.MIXTO)
                .build();

        when(convocatoriaService.getDetalleConvocatoria(id)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/convocatorias/{id}", id)
                        // Usamos ROLE_SOCIAL_OUTREACH_SECRETARY para lectura como solicitado
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOCIAL_OUTREACH_SECRETARY")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Consulta exitosa"))
                .andExpect(jsonPath("$.data.datosGeneralesConvocatoria.mes").value("ENERO"))
                .andExpect(jsonPath("$.data.cantidadPostulantes").value(100))
                .andExpect(jsonPath("$.data.tasaVacantesCubiertas").value("80%")) // Verificación del nuevo campo
                .andExpect(jsonPath("$.data.modoEvaluacion").value("MIXTO"));

        verify(convocatoriaService, times(1)).getDetalleConvocatoria(id);
    }

    @Test
    void getDetallesConvocatoria_ShouldReturnForbidden_WhenRoleIsInvalid() throws Exception {
        // Arrange
        Integer id = 1;

        // Act & Assert
        mockMvc.perform(get("/api/v1/convocatorias/{id}", id)
                        // Rol incorrecto para probar 403
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(convocatoriaService, times(0)).getDetalleConvocatoria(id);
    }

    @Test
    void getDetallesConvocatoria_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
        // Arrange
        Integer id = 1;

        // Act & Assert
        // Sin .with(jwt()) para probar 401
        mockMvc.perform(get("/api/v1/convocatorias/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(convocatoriaService, times(0)).getDetalleConvocatoria(id);
    }

    @Test
    void getDetallesConvocatoria_ShouldReturnNotFound_WhenIdDoesNotExist() throws Exception {
        // Arrange
        Integer id = 999;
        when(convocatoriaService.getDetalleConvocatoria(id))
                .thenThrow(new NotFoundException("No se encontró convocatoria con el id especificado"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/convocatorias/{id}", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOCIAL_OUTREACH_SECRETARY")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(convocatoriaService, times(1)).getDetalleConvocatoria(id);
    }

    @Test
    void getConvocatoriaAbierta_ShouldReturnOk_WhenAnonymous() throws Exception {
        // Arrange
        ConvocatoriaAbiertaResponse response = ConvocatoriaAbiertaResponse.builder()
                .mes(Mes.ENERO)
                .cantidadVacantes(10)
                .build();

        when(convocatoriaService.getConvocatoriaAbierta()).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/convocatorias/activa")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Consulta exitosa"))
                .andExpect(jsonPath("$.data.mes").value("ENERO"))
                .andExpect(jsonPath("$.data.cantidadVacantes").value(10));

        verify(convocatoriaService, times(1)).getConvocatoriaAbierta();
    }

    @Test
    void getHistorialConvocatorias_ShouldReturnOk_WhenAuthorized() throws Exception {
        // Arrange
        Integer year = 2023;
        HistorialConvocatoriaResponse historial = HistorialConvocatoriaResponse.builder()
                .mes(Mes.ENERO)
                .build();
        List<HistorialConvocatoriaResponse> response = List.of(historial);

        when(convocatoriaService.getHistorialConvocatorias(year)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/convocatorias/historial")
                        .param("year", year.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOCIAL_OUTREACH_SECRETARY")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Consulta exitosa"))
                .andExpect(jsonPath("$.data[0].mes").value("ENERO"));

        verify(convocatoriaService, times(1)).getHistorialConvocatorias(year);
    }

    @Test
    void getHistorialConvocatorias_ShouldReturnForbidden_WhenRoleIsInvalid() throws Exception {
        // Arrange
        Integer year = 2023;

        // Act & Assert
        mockMvc.perform(get("/api/v1/convocatorias/historial")
                        .param("year", year.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(convocatoriaService, times(0)).getHistorialConvocatorias(year);
    }

    @Test
    void getHistorialConvocatorias_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
        // Arrange
        Integer year = 2023;

        // Act & Assert
        mockMvc.perform(get("/api/v1/convocatorias/historial")
                        .param("year", year.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(convocatoriaService, times(0)).getHistorialConvocatorias(year);
    }

    @Test
    void actualizarEstadoConvocatoria_Exitoso() throws Exception {
        // Arrange
        UpdateEstadoConvocatoriaRequest request = new UpdateEstadoConvocatoriaRequest();
        request.setIdConvocatoria(1);
        request.setEstadoConvocatoria(EstadoConvocatoria.CERRADO);

        HistorialConvocatoriaResponse datosGenerales = HistorialConvocatoriaResponse.builder()
                .mes(Mes.ENERO)
                .estado(EstadoConvocatoria.CERRADO)
                .build();

        DetalleConvocatoriaResponse response = DetalleConvocatoriaResponse.builder()
                .datosGeneralesConvocatoria(datosGenerales)
                .tasaVacantesCubiertas("100%")
                .build();

        when(convocatoriaService.actualizarEstadoConvocatoria(any(UpdateEstadoConvocatoriaRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/convocatorias/estado")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOCIAL_OUTREACH_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Actualización exitosa"))
                .andExpect(jsonPath("$.data.tasaVacantesCubiertas").value("100%"))
                .andExpect(jsonPath("$.data.datosGeneralesConvocatoria.estado").value("CERRADO"));

        verify(convocatoriaService, times(1)).actualizarEstadoConvocatoria(any(UpdateEstadoConvocatoriaRequest.class));
    }

    @Test
    void actualizarEstadoConvocatoria_Forbidden() throws Exception {
        // Arrange
        UpdateEstadoConvocatoriaRequest request = new UpdateEstadoConvocatoriaRequest();
        request.setIdConvocatoria(1);
        request.setEstadoConvocatoria(EstadoConvocatoria.CERRADO);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/convocatorias/estado")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(convocatoriaService, times(0)).actualizarEstadoConvocatoria(any(UpdateEstadoConvocatoriaRequest.class));
    }

    @Test
    void actualizarEstadoConvocatoria_BadRequest() throws Exception {
        // Arrange
        UpdateEstadoConvocatoriaRequest request = new UpdateEstadoConvocatoriaRequest();
        // Leaving fields null to trigger @Valid

        // Act & Assert
        mockMvc.perform(patch("/api/v1/convocatorias/estado")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOCIAL_OUTREACH_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(convocatoriaService, times(0)).actualizarEstadoConvocatoria(any(UpdateEstadoConvocatoriaRequest.class));
    }

    @Test
    void listarPostulantes_Exitoso() throws Exception {
        // Arrange
        Integer idConvocatoria = 1;

        PostulanteConvocatoriaResponse response = PostulanteConvocatoriaResponse.builder()
                .idEstudiante(UUID.randomUUID())
                .codigo("STU001")
                .nombreCompleto("Juan Perez")
                .becado(true)
                .promedioGeneral(18.5)
                .fechaPostulacion(LocalDate.now())
                .build();

        Page<PostulanteConvocatoriaResponse> pageResponse = new PageImpl<>(List.of(response));

        when(postulacionService.obtenerPostulantesConvocatoria(eq(idConvocatoria), any(Pageable.class)))
                .thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/convocatorias/{id}/postulantes", idConvocatoria)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOCIAL_OUTREACH_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Consulta exitosa"))
                .andExpect(jsonPath("$.data.content[0].nombreCompleto").value("Juan Perez"));

        verify(postulacionService).obtenerPostulantesConvocatoria(eq(idConvocatoria), any(Pageable.class));
    }

    @Test
    void listarPostulantes_Forbidden() throws Exception {
        // Arrange
        Integer idConvocatoria = 1;

        // Act & Assert
        mockMvc.perform(get("/api/v1/convocatorias/{id}/postulantes", idConvocatoria)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(postulacionService, times(0)).obtenerPostulantesConvocatoria(any(), any());
    }

    @Test
    void listarPostulantes_BadRequest_OrdenInvalido() throws Exception {
        // Arrange
        Integer idConvocatoria = 1;
        String invalidSort = "contraseña";

        when(postulacionService.obtenerPostulantesConvocatoria(eq(idConvocatoria), any(Pageable.class)))
                .thenThrow(new BadRequestException("No se puede ordenar por el campo: " + invalidSort));

        // Act & Assert
        mockMvc.perform(get("/api/v1/convocatorias/{id}/postulantes", idConvocatoria)
                        .param("sort", invalidSort)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOCIAL_OUTREACH_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(postulacionService, times(1)).obtenerPostulantesConvocatoria(eq(idConvocatoria), any(Pageable.class));
    }
}
