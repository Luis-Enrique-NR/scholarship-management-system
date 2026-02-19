package pe.com.security.scholarship.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BusinessException {
  public ForbiddenException(String message) {
    super(message);
  }

  public HttpStatus getHttpStatus() {
    return HttpStatus.FORBIDDEN;
  }
}