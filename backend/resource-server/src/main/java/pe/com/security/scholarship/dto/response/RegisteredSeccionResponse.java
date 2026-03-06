package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class RegisteredSeccionResponse {
  private Integer id;
  private LocalDate fechaInicio;
  private Integer vacantesDisponibles;
  private Instant createdAt;
  private Instant updatedAt;
  private List<RegisteredHorarioResponse> horarios;
}
