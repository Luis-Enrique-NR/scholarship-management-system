package pe.com.security.scholarship.util;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import pe.com.security.scholarship.dto.CsvImportRequest;
import pe.com.security.scholarship.dto.ProcesamientoResult;
import pe.com.security.scholarship.exception.BadRequestException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Component
public class CargaMasivaHelper {

  public <T extends CsvImportRequest> ProcesamientoResult procesar(
          MultipartFile file,
          Class<T> clase,
          Consumer<T> logicaNegocio) { // Consumer es "qué hacer con cada fila"

    List<T> filas = leerCsv(file, clase);
    int exitos = 0;
    int fallidos = 0;
    List<ProcesamientoResult.ErrorDetalle> errores = new ArrayList<>();

    for (T fila : filas) {
      try {
        logicaNegocio.accept(fila); // Aquí se ejecuta la lógica del Service
        exitos++;
      } catch (Exception e) {
        fallidos++;
        errores.add(new ProcesamientoResult.ErrorDetalle(fila.getIdentifier(), e.getMessage()));
      }
    }

    return ProcesamientoResult.builder()
            .total(filas.size())
            .exitos(exitos)
            .fallidos(fallidos)
            .errores(errores)
            .build();
  }

  private <T> List<T> leerCsv(MultipartFile file, Class<T> clase) {
    try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
      HeaderColumnNameMappingStrategy<T> strategy = new HeaderColumnNameMappingStrategy<>();
      strategy.setType(clase);

      CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(reader)
              .withType(clase)
              .withMappingStrategy(strategy) // Usar la estrategia personalizada
              .withSeparator(';')
              .withIgnoreLeadingWhiteSpace(true)
              .withIgnoreEmptyLine(true)
              .withThrowExceptions(true) // Forzar a que lance excepciones si algo falla
              .build();

      return csvToBean.parse();
    } catch (IOException e) {
      // Error físico de lectura del archivo
      throw new BadRequestException("No se pudo leer el archivo: " + e.getMessage());

    } catch (RuntimeException e) {
      Throwable cause = e.getCause();

      if (cause instanceof com.opencsv.exceptions.CsvDataTypeMismatchException mismatchEx) {
        throw new BadRequestException("Error de tipo de dato en la línea " + mismatchEx.getLineNumber() +
                ": El valor '" + mismatchEx.getDestinationClass().getSimpleName() + "' no es válido.");
      }

      if (cause instanceof com.opencsv.exceptions.CsvRequiredFieldEmptyException requiredEx) {
        throw new BadRequestException("Error en la línea " + requiredEx.getLineNumber() +
                ": La columna '" + requiredEx.getDestinationField().getName() + "' es obligatoria.");
      }

      if (cause instanceof com.opencsv.exceptions.CsvException csvEx) {
        throw new BadRequestException("Error de formato en la línea " + csvEx.getLineNumber() + ": " + csvEx.getMessage());
      }

      // Si es otra RuntimeException que no conocemos
      throw new BadRequestException("El archivo contiene errores estructurales: " + e.getMessage());
    }
  }
}