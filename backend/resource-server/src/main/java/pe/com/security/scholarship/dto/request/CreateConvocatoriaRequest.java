package pe.com.security.scholarship.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateConvocatoriaRequest {

  private String mes;

  private LocalDate fechaInicio;

  private LocalDate fechaFin;
}
