package pe.com.security.scholarship.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import pe.com.security.scholarship.domain.enums.EstadoConvocatoria;
import pe.com.security.scholarship.domain.enums.Mes;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class HistorialConvocatoriaResponse {
  private Integer id;
  private Mes mes;
  private LocalDate fechaInicio;
  private LocalDate fechaFin;
  private EstadoConvocatoria estado;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "America/Lima")
  private Instant createdAt;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "America/Lima")
  private Instant updatedAt;
}
