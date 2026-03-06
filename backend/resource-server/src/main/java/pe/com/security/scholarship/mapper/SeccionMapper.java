package pe.com.security.scholarship.mapper;

import pe.com.security.scholarship.domain.entity.Curso;
import pe.com.security.scholarship.domain.entity.Seccion;
import pe.com.security.scholarship.dto.response.RegisteredSeccionResponse;
import pe.com.security.scholarship.dto.response.UpdatedVacantesSeccionResponse;

import java.time.Instant;
import java.time.LocalDate;

public class SeccionMapper {

  public static Seccion buildSeccion(LocalDate fechaInicio, Curso curso, Integer vacantes) {
    return Seccion.builder()
            .fechaInicio(fechaInicio)
            .curso(curso)
            .vacantesDisponibles(vacantes)
            .createdAt(Instant.now())
            .build();
  }

  public static RegisteredSeccionResponse mapRegisteredSeccion(Seccion seccion) {
    return RegisteredSeccionResponse.builder()
            .id(seccion.getId())
            .createdAt(seccion.getCreatedAt())
            .updatedAt(seccion.getUpdatedAt())
            .fechaInicio(seccion.getFechaInicio())
            .vacantesDisponibles(seccion.getVacantesDisponibles())
            .horarios(seccion.getHorarios().stream().map(HorarioMapper::mapRegisteredHorario).toList())
            .build();
  }

  public static UpdatedVacantesSeccionResponse mapUpdatedVacantes(int cantidadNuevosMatriculados, int totalMatriculados) {
    return UpdatedVacantesSeccionResponse.builder()
            .cantidadNuevosMatriculados(cantidadNuevosMatriculados)
            .totalMatriculados(totalMatriculados)
            .build();
  }
}
