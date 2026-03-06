package pe.com.security.scholarship.mapper;

import pe.com.security.scholarship.domain.entity.Curso;
import pe.com.security.scholarship.domain.enums.ModalidadCurso;
import pe.com.security.scholarship.dto.response.CursoPostulacionResponse;
import pe.com.security.scholarship.dto.response.RegisteredCursoResponse;

import java.time.Instant;

public class CursoMapper {

  public static Curso buildCurso(String nombre, String codigo, ModalidadCurso modalidadCurso) {
    return Curso.builder()
            .nombre(nombre)
            .codigo(codigo)
            .modalidad(modalidadCurso)
            .createdAt(Instant.now())
            .build();
  }

  public static RegisteredCursoResponse mapRegisteredCurso(Curso curso) {
    return RegisteredCursoResponse.builder()
            .id(curso.getId())
            .createdAt(curso.getCreatedAt())
            .updatedAt(curso.getUpdatedAt())
            .codigo(curso.getCodigo())
            .nombre(curso.getNombre())
            .modalidad(curso.getModalidad())
            .secciones(curso.getSecciones().stream().map(SeccionMapper::mapRegisteredSeccion).toList())
            .build();
  }

  public static CursoPostulacionResponse mapCursoPostulacion(Curso curso) {
    return CursoPostulacionResponse.builder()
            .nombre(curso.getNombre())
            .codigo(curso.getCodigo())
            .modalidad(curso.getModalidad().name())
            .build();
  }
}
