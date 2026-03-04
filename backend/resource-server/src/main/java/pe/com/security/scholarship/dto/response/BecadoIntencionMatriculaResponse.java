package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;
import pe.com.security.scholarship.domain.enums.EstadoMatricula;

@Data
@Builder
public class BecadoIntencionMatriculaResponse {
  private Integer idPostulacion;
  private String nombreCompleto;
  private String codigo;
  private Double promedioGeneral;
  private EstadoMatricula estadoMatricula;
}
