package pe.com.security.scholarship.util;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse<T> {
  private String message; // Mensaje descriptivo (por ejemplo, "Éxito", "Error", etc.)
  private String codigo; // Especificar el resultado de la operación Restful
  private T data;         // Datos de la respuesta (puede ser cualquier tipo: Page, List, etc.)
}