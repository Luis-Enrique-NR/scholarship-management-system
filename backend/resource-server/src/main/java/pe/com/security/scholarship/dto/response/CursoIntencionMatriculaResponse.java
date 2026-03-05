package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CursoIntencionMatriculaResponse {
  private Integer idCurso;
  private String nombreCurso;
  private String codigoCurso;
  private List<SeccionIntencionMatriculaResponse> secciones;
}
