package pe.com.security.scholarship.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdatePasswordRequest {

  @NotBlank(message = "Contraseña actual obligatoria")
  private String oldPassword;
  @NotBlank(message = "Nueva contraseña obligatoria")
  private String newPassword;
}
