package pe.com.security.scholarship.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Postulacion {

  private Long id;

  private String cursoOpcion;

  private LocalDateTime fechaPostulacion;

  private Convocatoria convocatoria;
}
