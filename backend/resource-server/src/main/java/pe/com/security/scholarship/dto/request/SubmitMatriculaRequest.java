package pe.com.security.scholarship.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class SubmitMatriculaRequest {
  @NotNull(message = "ID de la sección obligatoria")
  @Positive(message = "ID de la sección debe ser un número positivo")
  private Integer idSeccion;
}
