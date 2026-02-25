package pe.com.security.scholarship.mapper;

import pe.com.security.scholarship.domain.entity.Empleado;
import pe.com.security.scholarship.domain.entity.Estudiante;
import pe.com.security.scholarship.domain.entity.PeriodoAcademico;
import pe.com.security.scholarship.domain.entity.PromedioPonderado;

import java.time.Instant;
import java.time.LocalDate;

public class PromedioPonderadoMapper {
  public static PromedioPonderado buildPromedioPonderado(Estudiante estudiante, PeriodoAcademico periodoAcademico,
                                                         Empleado empleado, Integer ciclo, Double promedio) {
    return PromedioPonderado.builder()
            .estudiante(estudiante)
            .cicloRelativo(ciclo)
            .promedioPonderado(promedio)
            .createdAt(Instant.now())
            .periodoAcademico(periodoAcademico)
            .empleado(empleado)
            .build();
  }
}
