package pe.com.security.scholarship.mapper;

import pe.com.security.scholarship.domain.entity.HorarioSeccion;
import pe.com.security.scholarship.domain.entity.Matricula;
import pe.com.security.scholarship.domain.entity.Postulacion;
import pe.com.security.scholarship.domain.entity.Seccion;
import pe.com.security.scholarship.domain.enums.EstadoMatricula;
import pe.com.security.scholarship.dto.response.DetalleHorarioMatriculaResponse;
import pe.com.security.scholarship.dto.response.RegisteredMatriculaResponse;

import java.time.Instant;

public class MatriculaMapper {

  public static Matricula buildMatricula(Seccion seccion, Postulacion postulacion) {
    return Matricula.builder()
            .postulacion(postulacion)
            .seccion(seccion)
            .fechaSolicitud(Instant.now())
            .estado(EstadoMatricula.PENDIENTE)
            .build();
  }

  public static RegisteredMatriculaResponse mapSubmitMatricula(Matricula matricula) {
    return RegisteredMatriculaResponse.builder()
            .idMatricula(matricula.getId())
            .estadoMatricula(matricula.getEstado())
            .nombreCurso(matricula.getSeccion().getCurso().getNombre())
            .fechaInicioSeccion(matricula.getSeccion().getFechaInicio())
            .horarioSeccion(matricula.getSeccion().getHorarios().stream().map(MatriculaMapper::mapDetalleHorario).toList())
            .build();
  }

  public static DetalleHorarioMatriculaResponse mapDetalleHorario(HorarioSeccion horarioSeccion) {
    return DetalleHorarioMatriculaResponse.builder()
            .dia(horarioSeccion.getDiaSemana())
            .horaInicio(horarioSeccion.getHoraInicio())
            .horaFin(horarioSeccion.getHoraFin())
            .build();
  }
}
