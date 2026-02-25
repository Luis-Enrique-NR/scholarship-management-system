package pe.com.security.scholarship.util;

import org.springframework.web.multipart.MultipartFile;
import pe.com.security.scholarship.exception.BadRequestException;

public class FileUtils {
  public static void validarCsv(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("El archivo no puede estar vacío");
    }

    String fileName = file.getOriginalFilename();
    if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
      throw new BadRequestException("Solo se permiten archivos en formato CSV");
    }
  }
}
