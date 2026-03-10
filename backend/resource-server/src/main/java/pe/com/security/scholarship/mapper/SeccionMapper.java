package pe.com.security.scholarship.mapper;

import pe.com.security.scholarship.domain.entity.Curso;
import pe.com.security.scholarship.domain.entity.HorarioSeccion;
import pe.com.security.scholarship.domain.entity.Seccion;
import pe.com.security.scholarship.dto.response.OverviewSeccionResponse;
import pe.com.security.scholarship.dto.response.RegisteredSeccionResponse;
import pe.com.security.scholarship.dto.response.UpdatedVacantesSeccionResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.stream.Collectors;

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

  public static OverviewSeccionResponse mapOverviewSeccion(Seccion seccion) {
    String horarios = seccion.getHorarios().stream()
            .sorted(Comparator
                    .comparing((HorarioSeccion h) -> h.getDiaSemana().ordinal())
                    .thenComparing(HorarioSeccion::getHoraInicio))
            .map(h -> String.format("%s %s-%s", h.getDiaSemana(), h.getHoraInicio(), h.getHoraFin()))
            .collect(Collectors.joining(", "));

    return OverviewSeccionResponse.builder()
            .id(seccion.getId())
            .horario(horarios)
            .fechaInicio(seccion.getFechaInicio())
            .build();
  }
}
