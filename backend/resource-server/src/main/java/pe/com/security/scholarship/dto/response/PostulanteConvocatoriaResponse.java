package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class PostulanteConvocatoriaResponse {
  private UUID idEstudiante;
  private String codigoEstudiante;
  private String nombreCompleto;
  private Double promedioGeneral;
  private LocalDate fechaPostulacion;
  private Boolean aceptado;
}
