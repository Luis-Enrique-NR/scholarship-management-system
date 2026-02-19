package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisteredUserResponse {
  private String correo;
  private String nombres;
  private String apellidos;
  private Boolean habilitado;
}
