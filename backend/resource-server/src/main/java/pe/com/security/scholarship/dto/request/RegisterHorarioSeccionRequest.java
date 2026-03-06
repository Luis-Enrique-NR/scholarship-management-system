package pe.com.security.scholarship.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import pe.com.security.scholarship.domain.enums.DiaSemana;

import java.time.LocalTime;

@Data
public class RegisterHorarioSeccionRequest {
  @NotNull(message = "El día de la semana es obligatorio")
  private DiaSemana diaSemana;
  @NotNull(message = "La hora de inicio es obligatoria")
  private LocalTime horaInicio;
  @NotNull(message = "La hora de fin es obligatoria")
  private LocalTime horaFin;
}
