package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;
import pe.com.security.scholarship.entity.enums.Mes;

import java.time.LocalDate;

@Data
@Builder
public class ConvocatoriaAbiertaResponse {
  private Mes mes;
  private LocalDate fechaInicio;
  private LocalDate fechaFin;
  private Integer cantidadVacantes;
}
