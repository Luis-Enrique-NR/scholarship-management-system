package pe.com.security.scholarship.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BusinessException {
  public ConflictException(String message) {
    super(message);
  }

  public HttpStatus getHttpStatus() {
    return HttpStatus.CONFLICT;
  }
}
