package pe.com.security.scholarship.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.com.security.scholarship.util.ApiResponse;

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
}
