package pe.com.security.scholarship.mapper;

import pe.com.security.scholarship.domain.entity.Empleado;
import pe.com.security.scholarship.domain.entity.Estudiante;
import pe.com.security.scholarship.domain.entity.EvaluacionSocioeconomica;
import pe.com.security.scholarship.domain.enums.NivelSocioeconomico;

import java.time.Instant;
import java.time.LocalDate;

public class EvaluacionSocioeconomicaMapper {

  public static EvaluacionSocioeconomica buildEvaluacionSocioeconomica(Estudiante estudiante, LocalDate fechaEvaluacion,
                                                                       NivelSocioeconomico nivel, Empleado empleado) {
    return EvaluacionSocioeconomica.builder()
            .estudiante(estudiante)
            .fechaEvaluacion(fechaEvaluacion)
            .fechaExpiracion(fechaEvaluacion.plusMonths(5))
            .nivelSocioeconomico(nivel)
            .createdAt(Instant.now())
            .createdBy(empleado)
            .build();
  }
}
