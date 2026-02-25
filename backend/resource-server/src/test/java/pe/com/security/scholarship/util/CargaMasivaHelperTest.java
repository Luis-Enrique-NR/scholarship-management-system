package pe.com.security.scholarship.util;

import com.opencsv.bean.CsvBindByName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import pe.com.security.scholarship.dto.CsvImportRequest;
import pe.com.security.scholarship.dto.ProcesamientoResult;
import pe.com.security.scholarship.exception.BadRequestException;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CargaMasivaHelperTest {

    @InjectMocks
    private CargaMasivaHelper cargaMasivaHelper;

    // DTO interno para pruebas
    public static class TestCsvRequest implements CsvImportRequest {
        @CsvBindByName(required = true)
        private String id;

        @CsvBindByName(required = true)
        private Integer valor;

        @Override
        public String getIdentifier() {
            return id;
        }

        // Getters y Setters necesarios para OpenCSV
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public Integer getValor() { return valor; }
        public void setValor(Integer valor) { this.valor = valor; }
    }

    @Test
    void testLeerCsvFallaPorCabecera() {
        // Arrange: CSV sin la columna 'valor'
        String csvContent = "id\n" +
                            "A1";
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // Act & Assert
        assertThatThrownBy(() -> cargaMasivaHelper.procesar(file, TestCsvRequest.class, req -> {}))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("La columna 'valor' es obligatoria");
    }

    @Test
    void testLeerCsvFallaPorTipoDato() {
        // Arrange: CSV con un String en la columna 'valor' (que espera Integer)
        String csvContent = "id;valor\n" +
                            "A1;TextoInvalido";
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // Act & Assert
        assertThatThrownBy(() -> cargaMasivaHelper.procesar(file, TestCsvRequest.class, req -> {}))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Error de tipo de dato");
    }

    @Test
    void testProcesarFlujoExitosoYFallido() {
        // Arrange: 3 filas, la segunda fallará en la lógica de negocio
        String csvContent = "id;valor\n" +
                            "A1;10\n" +
                            "A2;20\n" +
                            "A3;30";
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        Consumer<TestCsvRequest> logicaNegocio = request -> {
            if ("A2".equals(request.getId())) {
                throw new RuntimeException("Error simulado");
            }
        };

        // Act
        ProcesamientoResult result = cargaMasivaHelper.procesar(file, TestCsvRequest.class, logicaNegocio);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(3);
        assertThat(result.getExitos()).isEqualTo(2);
        assertThat(result.getFallidos()).isEqualTo(1);
        
        assertThat(result.getErrores()).hasSize(1);
        assertThat(result.getErrores().get(0).getIdentifier()).isEqualTo("A2");
        assertThat(result.getErrores().get(0).getMensaje()).isEqualTo("Error simulado");
    }
}
