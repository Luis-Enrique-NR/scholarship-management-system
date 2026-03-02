package pe.com.security.scholarship.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import pe.com.security.scholarship.domain.enums.EstadoMatricula;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class IntencionMatriculaResponse {
  private Integer idMatricula;
  private EstadoMatricula estadoMatricula;
  private LocalDate fechaSolicitud;
  private String nombreCurso;
  private LocalDate fechaInicioSeccion;
  private List<DetalleHorarioMatriculaResponse> horarioSeccion;
  private LocalDate fechaMatricula;
  private Double notaMatricula;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "America/Lima")
  private Instant updatedAt;
}
