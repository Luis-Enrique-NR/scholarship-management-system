package pe.com.security.scholarship.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CargaEvaluacionesResponse {
  private int totalProcesados;
  private int totalExitosos;
  private int totalFallidos;
  private List<ErrorDetalle> errores;

  @Data
  @AllArgsConstructor
  public static class ErrorDetalle {
    private String codigoEstudiante;
    private String motivo;
  }
}
