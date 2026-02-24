package pe.com.security.scholarship.mapper;

import pe.com.security.scholarship.domain.entity.Convocatoria;
import pe.com.security.scholarship.domain.entity.Curso;
import pe.com.security.scholarship.domain.entity.Estudiante;
import pe.com.security.scholarship.domain.entity.Postulacion;
import pe.com.security.scholarship.dto.response.CursoPostulacionResponse;
import pe.com.security.scholarship.dto.response.RegisteredPostulacionResponse;

import java.time.LocalDate;
import java.util.Set;

public class PostulacionMapper {

  public static Postulacion buildPostulacion(Convocatoria convocatoria, Estudiante estudiante, Set<Curso> cursos) {
    return Postulacion.builder()
            .estudiante(estudiante)
            .convocatoria(convocatoria)
            .fechaPostulacion(LocalDate.now())
            .cursos(cursos)
            .build();
  }

  public static RegisteredPostulacionResponse mapRegisteredPostulacion(Postulacion postulacion,
                                                                       Set<CursoPostulacionResponse> cursos) {
    return RegisteredPostulacionResponse.builder()
            .id(postulacion.getId())
            .fechaPostulacion(postulacion.getFechaPostulacion())
            .idConvocatoria(postulacion.getConvocatoria().getId())
            .cursos(cursos)
            .build();
  }
}
