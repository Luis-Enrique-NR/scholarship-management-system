package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class InformacionPostulacionResponse {
  private String mesConvocatoria;
  private String estadoPostulacion;
  private LocalDate fechaPostulacion;
  private Double promedioGeneral;
  private List<String> cursosOpciones;
}
