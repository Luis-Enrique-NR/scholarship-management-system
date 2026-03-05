package pe.com.security.scholarship.dto.projection;

import java.time.LocalDate;

public interface SeccionIntencionProjection {
  Integer getIdSeccion();
  LocalDate getFechaInicio();
  Integer getIdCurso();
  String getNombreCurso();
  String getCodigoCurso();
  Integer getTotalIntencionesPendientes();
}
