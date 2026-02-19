package pe.com.security.scholarship.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends BusinessException {
  public BadRequestException(String message) {
    super(message);
  }

  public HttpStatus getHttpStatus() {
    return HttpStatus.BAD_REQUEST;
  }
}