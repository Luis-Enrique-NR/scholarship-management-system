package pe.com.security.scholarship.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.security.scholarship.config.ResourceServerTest;
import pe.com.security.scholarship.dto.ProcesamientoResult;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.service.EvaluacionSocioeconomicaService;
import pe.com.security.scholarship.util.FileUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest(EvaluacionSocioeconomicaController.class)
@ResourceServerTest
class EvaluacionSocioeconomicaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EvaluacionSocioeconomicaService evaluacionService;

    @AfterEach
    void tearDown() {
        // No static mocks to close here as we use try-with-resources in each test
    }

    @Test
    void uploadEvaluaciones_Exito() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "content".getBytes());
        ProcesamientoResult result = ProcesamientoResult.builder().total(1).exitos(1).build();

        try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class)) {
            // Mockeamos que la validación pase sin errores
            fileUtilsMock.when(() -> FileUtils.validarCsv(any())).thenAnswer(invocation -> null);
            
            when(evaluacionService.procesarCargaMasiva(any())).thenReturn(result);

            // Act & Assert
            mockMvc.perform(multipart("/api/v1/evaluaciones/upload")
                            .file(file)
                            .with(csrf())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOCIAL_OUTREACH_SECRETARY")))
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Procesamiento finalizado"))
                    .andExpect(jsonPath("$.data.total").value(1));

            verify(evaluacionService, times(1)).procesarCargaMasiva(any());
        }
    }

    @Test
    void uploadEvaluaciones_ArchivoInvalido() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());

        try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class)) {
            // Mockeamos que la validación falle
            fileUtilsMock.when(() -> FileUtils.validarCsv(any()))
                    .thenThrow(new BadRequestException("Solo se permiten archivos en formato CSV"));

            // Act & Assert
            mockMvc.perform(multipart("/api/v1/evaluaciones/upload")
                            .file(file)
                            .with(csrf())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOCIAL_OUTREACH_SECRETARY")))
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isBadRequest());
                    //.andExpect(jsonPath("$.message").value("Solo se permiten archivos en formato CSV"));

            verify(evaluacionService, times(0)).procesarCargaMasiva(any());
        }
    }

    @Test
    void uploadEvaluaciones_SinPermisos() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "content".getBytes());

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/evaluaciones/upload")
                        .file(file)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))) // Rol incorrecto
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isForbidden());

        verify(evaluacionService, times(0)).procesarCargaMasiva(any());
    }

    @Test
    void uploadEvaluaciones_ServiceError() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "content".getBytes());

        try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.validarCsv(any())).thenAnswer(invocation -> null);
            
            when(evaluacionService.procesarCargaMasiva(any()))
                    .thenThrow(new NotFoundException("Empleado no encontrado"));

            // Act & Assert
            mockMvc.perform(multipart("/api/v1/evaluaciones/upload")
                            .file(file)
                            .with(csrf())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOCIAL_OUTREACH_SECRETARY")))
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isNotFound());
                    //.andExpect(jsonPath("$.message").value("Empleado no encontrado"));

            verify(evaluacionService, times(1)).procesarCargaMasiva(any());
        }
    }
}
