package pe.com.security.scholarship.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.security.scholarship.config.ResourceServerTest;
import pe.com.security.scholarship.dto.ProcesamientoResult;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.service.PromedioPonderadoService;
import pe.com.security.scholarship.util.FileUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PromedioPonderadoController.class)
@ResourceServerTest
class PromedioPonderadoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PromedioPonderadoService promedioService;

    @AfterEach
    void tearDown() {
        // No static mocks to close here as we use try-with-resources in each test
    }

    @Test
    void testUploadPromedios_Exito() throws Exception {
        // Arrange
        String periodo = "2025-1";
        MockMultipartFile file = new MockMultipartFile("file", "promedios.csv", "text/csv", "content".getBytes());
        ProcesamientoResult result = ProcesamientoResult.builder().total(10).exitos(10).fallidos(0).build();

        try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.validarCsv(any())).thenAnswer(invocation -> null);
            
            when(promedioService.procesarCargaPromedios(any(), eq(periodo))).thenReturn(result);

            // Act & Assert
            mockMvc.perform(multipart("/api/v1/promedios/upload/{periodo}", periodo)
                            .file(file)
                            .with(csrf())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINING_CENTER_SECRETARY")))
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Procesamiento finalizado"))
                    .andExpect(jsonPath("$.data.total").value(10))
                    .andExpect(jsonPath("$.data.exitos").value(10));

            verify(promedioService, times(1)).procesarCargaPromedios(any(), eq(periodo));
        }
    }

    @Test
    void testUploadPromedios_ArchivoInvalido() throws Exception {
        // Arrange
        String periodo = "2025-1";
        MockMultipartFile file = new MockMultipartFile("file", "promedios.pdf", "application/pdf", "content".getBytes());

        try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.validarCsv(any()))
                    .thenThrow(new BadRequestException("Solo se permiten archivos en formato CSV"));

            // Act & Assert
            mockMvc.perform(multipart("/api/v1/promedios/upload/{periodo}", periodo)
                            .file(file)
                            .with(csrf())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINING_CENTER_SECRETARY")))
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isBadRequest());

            verify(promedioService, times(0)).procesarCargaPromedios(any(), any());
        }
    }

    @Test
    void testUploadPromedios_PeriodoNoExistente() throws Exception {
        // Arrange
        String periodo = "2099-1";
        MockMultipartFile file = new MockMultipartFile("file", "promedios.csv", "text/csv", "content".getBytes());

        try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.validarCsv(any())).thenAnswer(invocation -> null);
            
            when(promedioService.procesarCargaPromedios(any(), eq(periodo)))
                    .thenThrow(new NotFoundException("Periodo académico no registrado"));

            // Act & Assert
            mockMvc.perform(multipart("/api/v1/promedios/upload/{periodo}", periodo)
                            .file(file)
                            .with(csrf())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINING_CENTER_SECRETARY")))
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isNotFound());

            verify(promedioService, times(1)).procesarCargaPromedios(any(), eq(periodo));
        }
    }

    @Test
    void testUploadPromedios_Prohibido() throws Exception {
        // Arrange
        String periodo = "2025-1";
        MockMultipartFile file = new MockMultipartFile("file", "promedios.csv", "text/csv", "content".getBytes());

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/promedios/upload/{periodo}", periodo)
                        .file(file)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))) // Rol incorrecto
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isForbidden());

        verify(promedioService, times(0)).procesarCargaPromedios(any(), any());
    }
}
