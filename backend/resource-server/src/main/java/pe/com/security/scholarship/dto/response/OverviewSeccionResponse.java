package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class OverviewSeccionResponse {
  private Integer id;
  private String horario;
  private LocalDate fechaInicio;
}
