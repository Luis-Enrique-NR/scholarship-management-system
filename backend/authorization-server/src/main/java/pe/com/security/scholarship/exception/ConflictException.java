package pe.com.learning.security.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BusinessException {
  public ConflictException(String message) {
    super(message);
  }

  public HttpStatus getHttpStatus() {
    return HttpStatus.CONFLICT;
  }
}
