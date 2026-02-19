package pe.com.learning.security.exception;

import org.springframework.http.HttpStatus;

public class UnprocessableEntityException extends BusinessException {
  public UnprocessableEntityException(String message) {
    super(message);
  }

  public HttpStatus getHttpStatus() {
    return HttpStatus.UNPROCESSABLE_ENTITY;
  }
}