package pe.com.security.scholarship.mapper;

import pe.com.security.scholarship.domain.entity.HorarioSeccion;
import pe.com.security.scholarship.domain.entity.Seccion;
import pe.com.security.scholarship.domain.enums.DiaSemana;
import pe.com.security.scholarship.dto.response.RegisteredHorarioResponse;

import java.time.LocalTime;

public class HorarioMapper {

  public static HorarioSeccion buildHorario(DiaSemana diaSemana, LocalTime horaInicio, LocalTime horaFin, Seccion seccion) {
    return HorarioSeccion.builder()
            .diaSemana(diaSemana)
            .horaInicio(horaInicio)
            .horaFin(horaFin)
            .seccion(seccion)
            .build();
  }

  public static RegisteredHorarioResponse mapRegisteredHorario(HorarioSeccion horarioSeccion) {
    return RegisteredHorarioResponse.builder()
            .id(horarioSeccion.getId())
            .diaSemana(horarioSeccion.getDiaSemana())
            .horaInicio(horarioSeccion.getHoraInicio())
            .horaFin(horarioSeccion.getHoraFin())
            .build();
  }
}
