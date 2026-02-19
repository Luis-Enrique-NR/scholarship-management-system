package pe.com.learning.security.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BusinessException {
  public NotFoundException(String message) {
    super(message);
  }

  public HttpStatus getHttpStatus() {
    return HttpStatus.NOT_FOUND;
  }
}