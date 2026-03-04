package pe.com.security.scholarship.dto.projection;

import pe.com.security.scholarship.domain.enums.EstadoMatricula;

public interface BecadoIntencionProjection {
  Integer getIdPostulacion();
  String getNombreCompleto();
  String getCodigo();
  Double getPromedioGeneral();
  EstadoMatricula getEstadoMatricula();
}
