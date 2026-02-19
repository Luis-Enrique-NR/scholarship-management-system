package pe.com.security.scholarship.exception;

import org.springframework.http.HttpStatus;

public class BadCredentialsException extends BusinessException {
  public BadCredentialsException(String message) {
    super(message);
  }

  public HttpStatus getHttpStatus() {
    return HttpStatus.UNAUTHORIZED;
  }
}
