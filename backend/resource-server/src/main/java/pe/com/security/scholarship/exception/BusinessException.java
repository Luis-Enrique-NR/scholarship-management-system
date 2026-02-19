package pe.com.security.scholarship.exception;

import org.springframework.http.HttpStatus;

public abstract class BusinessException extends RuntimeException{
  public BusinessException(String message) {
    super(message);
  }
  public abstract HttpStatus getHttpStatus();
}
