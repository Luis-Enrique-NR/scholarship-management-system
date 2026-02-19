package pe.com.learning.security.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.com.learning.security.util.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Object>> handleBusiness(BusinessException e) {
    ApiResponse<Object> response = new ApiResponse<>(
            e.getMessage(),
            String.valueOf(e.getHttpStatus().value()),
            null
    );
    return ResponseEntity.status(e.getHttpStatus()).body(response);
  }

  /*
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Object>> handleSpringAccessDenied(AccessDeniedException e) {

    HttpStatus status = HttpStatus.FORBIDDEN;

    ApiResponse<Object> response = new ApiResponse<>(
            "Acceso denegado: " + e.getMessage(),
            String.valueOf(status.value()),
            null
    );
    return ResponseEntity.status(status).body(response);
  }

   */
}
