package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BecadoIntencionMatriculaResponse {
  private String nombreCompleto;
  private String codigo;
  private Double promedioGeneral;
}
