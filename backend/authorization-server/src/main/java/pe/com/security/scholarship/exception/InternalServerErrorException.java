package pe.com.learning.security.exception;

import org.springframework.http.HttpStatus;

public class InternalServerErrorException extends BusinessException {
  public InternalServerErrorException(String message) {
    super(message);
  }

  public HttpStatus getHttpStatus() {
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }
}