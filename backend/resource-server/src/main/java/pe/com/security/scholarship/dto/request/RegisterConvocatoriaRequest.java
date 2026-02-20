package pe.com.security.scholarship.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import pe.com.security.scholarship.entity.enums.Mes;

import java.time.LocalDate;

@Data
public class RegisterConvocatoriaRequest {
  @NotNull(message = "Mes obligatorio")
  private Mes mes;
  @NotNull(message = "Fecha de inicio obligatoria")
  @FutureOrPresent(message = "La fecha de inicio debe ser hoy o en el futuro")
  private LocalDate fechaInicio;
  @NotNull(message = "Fecha de fin obligatoria")
  @Future(message = "La fecha de fin debe ser una fecha futura")
  private LocalDate fechaFin;
  @NotNull(message = "El campo es obligatorio")
  @Positive(message = "Debe ser un número positivo")
  private Integer cantidadVacantes;
}
