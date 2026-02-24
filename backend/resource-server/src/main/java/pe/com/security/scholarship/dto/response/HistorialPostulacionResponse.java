package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class HistorialPostulacionResponse {
  private Integer id;
  private String estado;
  private LocalDate fechaPostulacion;
  private String mesConvocatoria;
}
