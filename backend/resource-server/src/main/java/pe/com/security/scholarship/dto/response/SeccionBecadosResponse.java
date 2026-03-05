package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class SeccionBecadosResponse {
  private Integer idSeccion;
  private LocalDate fechaInicio;
  private Integer vacantesTotales;
  private Integer vacantesDisponibles;
  private List<BecadoIntencionMatriculaResponse> becados;
}
