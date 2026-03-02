package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;
import pe.com.security.scholarship.domain.enums.DiaSemana;

import java.time.LocalTime;

@Data
@Builder
public class DetalleHorarioMatriculaResponse {
  private DiaSemana dia;
  private LocalTime horaInicio;
  private LocalTime horaFin;
}
