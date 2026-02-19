package pe.com.security.scholarship.exception;

import org.springframework.http.HttpStatus;

public class UnprocessableEntityException extends BusinessException {
  public UnprocessableEntityException(String message) {
    super(message);
  }

  public HttpStatus getHttpStatus() {
    return HttpStatus.UNPROCESSABLE_ENTITY;
  }
}