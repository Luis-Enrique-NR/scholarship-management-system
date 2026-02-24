package pe.com.security.scholarship.mapper;

import pe.com.security.scholarship.domain.entity.Convocatoria;
import pe.com.security.scholarship.domain.entity.Curso;
import pe.com.security.scholarship.domain.entity.Estudiante;
import pe.com.security.scholarship.domain.entity.Postulacion;
import pe.com.security.scholarship.dto.response.ConsultaPostulacionResponse;
import pe.com.security.scholarship.dto.response.CursoPostulacionResponse;
import pe.com.security.scholarship.dto.response.HistorialPostulacionResponse;
import pe.com.security.scholarship.dto.response.RegisteredPostulacionResponse;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

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

  public static ConsultaPostulacionResponse mapConsultaPostulacion(Postulacion postulacion) {
    return ConsultaPostulacionResponse.builder()
            .id(postulacion.getId())
            .estado(getEstadoPostulacion(postulacion))
            .convocatoria(ConvocatoriaMapper.mapHistorialConvocatoria(postulacion.getConvocatoria()))
            .cursos(postulacion.getCursos().stream().map(CursoMapper::mapCursoPostulacion).collect(Collectors.toSet()))
            .fechaPostulacion(postulacion.getFechaPostulacion())
            .build();
  }

  public static HistorialPostulacionResponse mapHistorialPostulacion(Postulacion postulacion) {
    return HistorialPostulacionResponse.builder()
            .id(postulacion.getId())
            .estado(getEstadoPostulacion(postulacion))
            .fechaPostulacion(postulacion.getFechaPostulacion())
            .mesConvocatoria(postulacion.getConvocatoria().getMes().name())
            .build();
  }

  public static String getEstadoPostulacion(Postulacion postulacion) {
    if (postulacion.getAceptado() == null) {
      return "Pendiente";
    } else if (postulacion.getAceptado()) {
      return "Aceptado";
    } else {
      return "Rechazado";
    }
  }
}
