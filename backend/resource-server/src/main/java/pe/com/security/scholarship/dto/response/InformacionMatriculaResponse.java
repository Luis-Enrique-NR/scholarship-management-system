package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class InformacionMatriculaResponse {
  private LocalDate fechaMatricula;
  private String cursoMatriculado;
  private Double notaMatricula;
}
