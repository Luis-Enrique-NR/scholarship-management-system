package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;
import pe.com.security.scholarship.domain.enums.EstadoMatricula;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class RegisteredMatriculaResponse {
  private Integer idMatricula;
  private EstadoMatricula estadoMatricula;
  private String nombreCurso;
  private LocalDate fechaInicioSeccion;
  private List<DetalleHorarioMatriculaResponse> horarioSeccion;
}
