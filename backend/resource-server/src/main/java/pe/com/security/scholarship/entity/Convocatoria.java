package pe.com.security.scholarship.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Convocatoria {

  private Long id;

  private String mes;

  private LocalDate fechaInicio;

  private LocalDate fechaFin;

  private String estado;
}
