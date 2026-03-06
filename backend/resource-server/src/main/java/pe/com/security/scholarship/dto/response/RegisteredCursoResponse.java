package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;
import pe.com.security.scholarship.domain.enums.ModalidadCurso;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class RegisteredCursoResponse {
  private Integer id;
  private String nombre;
  private String codigo;
  private ModalidadCurso modalidad;
  private Instant createdAt;
  private Instant updatedAt;
  private List<RegisteredSeccionResponse> secciones;
}
