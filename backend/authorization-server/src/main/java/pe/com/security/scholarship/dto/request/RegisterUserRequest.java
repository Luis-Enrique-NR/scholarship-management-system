package pe.com.security.scholarship.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterUserRequest {
  @NotBlank(message = "Nombres obligatorios")
  private String nombres;
  @NotBlank(message = "Apellidos obligatorios")
  private String apellidos;
  @NotBlank(message = "Correo obligatorio")
  @Email(message = "Formato inválido")
  private String correo;
  private String password;
}
