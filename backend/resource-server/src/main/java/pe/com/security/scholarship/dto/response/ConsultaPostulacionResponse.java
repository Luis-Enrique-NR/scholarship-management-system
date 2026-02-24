package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
public class ConsultaPostulacionResponse {
  private Integer id;
  private String estado;
  private HistorialConvocatoriaResponse convocatoria;
  private Set<CursoPostulacionResponse> cursos;
  private LocalDate fechaPostulacion;
}
