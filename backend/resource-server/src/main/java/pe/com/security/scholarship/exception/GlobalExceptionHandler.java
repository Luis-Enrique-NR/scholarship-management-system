package pe.com.security.scholarship.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.com.security.scholarship.util.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

  // 1. Maneja tus errores manuales (Negocio)
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Object>> handleBusiness(BusinessException e) {
    return buildResponse(e.getMessage(), e.getHttpStatus());
  }

  // 2. Maneja los errores automáticos (Validaciones de Spring)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException e) {
    // Extraemos el mensaje de la anotación (ej: "Debe ser un número positivo")
    String errorMsg = e.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
    return buildResponse(errorMsg, HttpStatus.BAD_REQUEST);
  }

  // Método privado para no repetir código de creación de ApiResponse
  private ResponseEntity<ApiResponse<Object>> buildResponse(String message, HttpStatus status) {
    ApiResponse<Object> response = new ApiResponse<>(
            message,
            String.valueOf(status.value()),
            null
    );
    return ResponseEntity.status(status).body(response);
  }
}
