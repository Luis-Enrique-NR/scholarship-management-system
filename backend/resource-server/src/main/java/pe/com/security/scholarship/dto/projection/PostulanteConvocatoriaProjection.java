package pe.com.security.scholarship.dto.projection;

import java.time.LocalDate;
import java.util.UUID;

public interface PostulanteConvocatoriaProjection {
  UUID getIdEstudiante();
  String getCodigo();
  String getNombreCompleto();
  Boolean getBecado();
  Double getPromedioGeneral();
  LocalDate getFechaPostulacion();
}
