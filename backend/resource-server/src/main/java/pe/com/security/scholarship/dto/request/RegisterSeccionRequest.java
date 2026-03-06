package pe.com.security.scholarship.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class RegisterSeccionRequest {
  @NotNull(message = "La fecha de inicio de la sección es obligatoria")
  private LocalDate fechaInicio;
  @PositiveOrZero(message = "La cantidad de vacantes disponibles no puede ser negativa")
  private Integer vacantesDisponibles;
  @Valid
  @NotEmpty(message = "Se debe asignar al menos un horario para la sección")
  private List<RegisterHorarioSeccionRequest> horarios;
}
