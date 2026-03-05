package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdatedVacantesSeccionResponse {
  private Integer cantidadNuevosMatriculados;
  private Integer totalMatriculados;
}
