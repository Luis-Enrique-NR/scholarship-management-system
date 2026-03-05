package pe.com.security.scholarship.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AprobarMatriculaRequest {
  @NotNull(message = "El ID de la matrícula es obligatorio")
  @Positive(message = "El ID de la matrícula debe ser un número positivo")
  private Integer idMatricula;
  @NotNull(message = "El estado de aprobación es obligatorio")
  private Boolean aprobado;
}
