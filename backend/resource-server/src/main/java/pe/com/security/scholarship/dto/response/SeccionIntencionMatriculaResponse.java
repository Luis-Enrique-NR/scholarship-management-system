package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class SeccionIntencionMatriculaResponse {
  private Integer idSeccion;
  private LocalDate fechaInicio;
  private Integer totalIntencionesMatricula;
}
