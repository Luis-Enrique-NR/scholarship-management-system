package pe.com.security.scholarship.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import pe.com.security.scholarship.domain.enums.EstadoConvocatoria;

@Data
public class UpdateEstadoConvocatoriaRequest {
  @NotNull(message = "ID de convocatoria obligatorio")
  private Integer idConvocatoria;
  @NotNull(message = "Estado de convocatoria obligatorio")
  private EstadoConvocatoria estadoConvocatoria;
}
