package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;
import pe.com.security.scholarship.domain.enums.DiaSemana;

import java.time.LocalTime;

@Data
@Builder
public class RegisteredHorarioResponse {
  private Integer id;
  private DiaSemana diaSemana;
  private LocalTime horaInicio;
  private LocalTime horaFin;
}
