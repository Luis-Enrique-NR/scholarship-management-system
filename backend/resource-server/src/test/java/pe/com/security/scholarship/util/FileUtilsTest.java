package pe.com.security.scholarship.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import pe.com.security.scholarship.exception.BadRequestException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUtilsTest {

    @Test
    void validarCsv_CuandoArchivoEsNulo() {
        // Act & Assert
        assertThatThrownBy(() -> FileUtils.validarCsv(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("El archivo no puede estar vacío");
    }

    @Test
    void validarCsv_CuandoArchivoEstaVacio() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", new byte[0]);

        // Act & Assert
        assertThatThrownBy(() -> FileUtils.validarCsv(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("El archivo no puede estar vacío");
    }

    @Test
    void validarCsv_CuandoExtensionNoEsCsv() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "datos.pdf", "application/pdf", "content".getBytes());

        // Act & Assert
        assertThatThrownBy(() -> FileUtils.validarCsv(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Solo se permiten archivos en formato CSV");
    }

    @Test
    void validarCsv_CuandoExtensionEsCsvValida() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "carga.csv", "text/csv", "content".getBytes());

        // Act & Assert
        assertThatCode(() -> FileUtils.validarCsv(file))
                .doesNotThrowAnyException();
    }

    @Test
    void validarCsv_CuandoExtensionEsCsvValidaMayusculas() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "CARGA.CSV", "text/csv", "content".getBytes());

        // Act & Assert
        assertThatCode(() -> FileUtils.validarCsv(file))
                .doesNotThrowAnyException();
    }
}
