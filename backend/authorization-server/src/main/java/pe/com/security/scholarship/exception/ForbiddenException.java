package pe.com.learning.security.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BusinessException {
  public ForbiddenException(String message) {
    super(message);
  }

  public HttpStatus getHttpStatus() {
    return HttpStatus.FORBIDDEN;
  }
}