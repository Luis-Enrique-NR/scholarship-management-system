package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
public class RegisteredPostulacionResponse {
  private Integer id;
  private LocalDate fechaPostulacion;
  private Integer idConvocatoria;
  private Set<CursoPostulacionResponse> cursos;
}
