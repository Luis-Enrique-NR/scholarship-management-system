package pe.com.security.scholarship.mapper;

import pe.com.security.scholarship.domain.entity.HorarioSeccion;
import pe.com.security.scholarship.domain.entity.Matricula;
import pe.com.security.scholarship.domain.entity.Postulacion;
import pe.com.security.scholarship.domain.entity.Seccion;
import pe.com.security.scholarship.domain.enums.EstadoMatricula;
import pe.com.security.scholarship.dto.projection.BecadoIntencionProjection;
import pe.com.security.scholarship.dto.projection.SeccionIntencionProjection;
import pe.com.security.scholarship.dto.response.BecadoIntencionMatriculaResponse;
import pe.com.security.scholarship.dto.response.CursoIntencionMatriculaResponse;
import pe.com.security.scholarship.dto.response.DetalleHorarioMatriculaResponse;
import pe.com.security.scholarship.dto.response.IntencionMatriculaResponse;
import pe.com.security.scholarship.dto.response.RegisteredMatriculaResponse;
import pe.com.security.scholarship.dto.response.SeccionIntencionMatriculaResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

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

  public static IntencionMatriculaResponse mapIntencionMatricula(Matricula matricula) {
    return IntencionMatriculaResponse.builder()
            .idMatricula(matricula.getId())
            .estadoMatricula(matricula.getEstado())
            .fechaSolicitud(LocalDate.ofInstant(matricula.getFechaSolicitud(), ZoneId.systemDefault()))
            .nombreCurso(matricula.getSeccion().getCurso().getNombre())
            .fechaInicioSeccion(matricula.getSeccion().getFechaInicio())
            .horarioSeccion(matricula.getSeccion().getHorarios().stream().map(MatriculaMapper::mapDetalleHorario).toList())
            // Los valores de aqui abajo pueden ser nulos
            .fechaMatricula(matricula.getFechaMatricula() != null
                    ? LocalDate.ofInstant(matricula.getFechaMatricula(), ZoneId.systemDefault())
                    : null)
            .notaMatricula(matricula.getNota())
            .updatedAt(matricula.getUpdatedAt())
            .build();
  }

  public static SeccionIntencionMatriculaResponse mapSeccionIntencionMatricula(SeccionIntencionProjection projection) {
    return SeccionIntencionMatriculaResponse.builder()
            .idSeccion(projection.getIdSeccion())
            .fechaInicio(projection.getFechaInicio())
            .totalIntencionesMatricula(projection.getTotalIntencionesMatricula())
            .build();
  }

  public static CursoIntencionMatriculaResponse mapCursoIntencionMatricula(SeccionIntencionProjection p) {
    return CursoIntencionMatriculaResponse.builder()
            .idCurso(p.getIdCurso())
            .nombreCurso(p.getNombreCurso())
            .codigoCurso(p.getCodigoCurso())
            .build();
  }

  public static BecadoIntencionMatriculaResponse mapBecadoIntencionMatricula(BecadoIntencionProjection projection) {
    return BecadoIntencionMatriculaResponse.builder()
            .idPostulacion(projection.getIdPostulacion())
            .nombreCompleto(projection.getNombreCompleto())
            .codigo(projection.getCodigo())
            .promedioGeneral(projection.getPromedioGeneral())
            .estadoMatricula(projection.getEstadoMatricula())
            .build();
  }
}
