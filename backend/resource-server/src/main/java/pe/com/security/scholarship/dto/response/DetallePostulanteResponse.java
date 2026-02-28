package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class DetallePostulanteResponse {
  private UUID idEstudiante;
  private String codigoEstudiante;
  private String nombreCompleto;
  List<ResultadoPostulacionResponse> postulaciones;
}
