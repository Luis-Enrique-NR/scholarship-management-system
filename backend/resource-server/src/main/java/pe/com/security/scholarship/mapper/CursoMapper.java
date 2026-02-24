package pe.com.security.scholarship.mapper;

import pe.com.security.scholarship.domain.entity.Curso;
import pe.com.security.scholarship.dto.response.CursoPostulacionResponse;

public class CursoMapper {

  public static CursoPostulacionResponse mapCursoPostulacion (Curso curso) {
    return CursoPostulacionResponse.builder()
            .nombre(curso.getNombre())
            .codigo(curso.getCodigo())
            .modalidad(curso.getModalidad().name())
            .build();
  }
}
