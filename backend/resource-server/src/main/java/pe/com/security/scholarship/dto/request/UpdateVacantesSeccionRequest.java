package pe.com.security.scholarship.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UpdateVacantesSeccionRequest {
  @NotNull(message = "El ID de la sección es requerido")
  @Positive(message = "El ID de la sección debe ser un número positivo")
  private Integer idSeccion;

  @NotNull(message = "La cantidad de vacantes es requerida")
  @Positive(message = "La cantidad de vacantes debe ser un número positivo")
  private Integer cantidadVacantes;
}
