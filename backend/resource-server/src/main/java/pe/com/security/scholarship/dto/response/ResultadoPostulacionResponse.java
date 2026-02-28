package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResultadoPostulacionResponse {
  // Información de Postulación
  private InformacionPostulacionResponse postulacion;
  // Información de Matrícula
  private InformacionMatriculaResponse matricula;
}
