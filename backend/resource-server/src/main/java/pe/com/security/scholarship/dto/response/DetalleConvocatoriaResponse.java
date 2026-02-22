package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;
import pe.com.security.scholarship.dto.projection.RankingProjection;

import java.util.List;

@Data
@Builder
public class DetalleConvocatoriaResponse {
  private HistorialConvocatoriaResponse datosGeneralesConvocatoria;

  private AuditEmpleadoResponse createdBy;
  private Integer cantidadVacantes;
  private Integer cantidadPostulantes;

  private String tasaAceptacion;
  private String tasaMatriculados;

  private List<RankingProjection> rankingSocioeconomico;
  private List<RankingProjection> rankingCiclos;
  private List<RankingProjection> rankingCarreras;
}
