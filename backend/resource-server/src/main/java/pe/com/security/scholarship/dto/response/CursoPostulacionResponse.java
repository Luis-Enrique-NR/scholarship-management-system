package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CursoPostulacionResponse {
  private String nombre;
  private String codigo;
  private String modalidad;
}
