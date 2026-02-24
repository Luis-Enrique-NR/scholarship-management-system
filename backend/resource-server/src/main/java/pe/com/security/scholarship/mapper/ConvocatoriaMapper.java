package pe.com.security.scholarship.mapper;

import pe.com.security.scholarship.dto.projection.RankingProjection;
import pe.com.security.scholarship.dto.projection.TasasConvocatoriaProjection;
import pe.com.security.scholarship.dto.request.RegisterConvocatoriaRequest;
import pe.com.security.scholarship.dto.response.AuditEmpleadoResponse;
import pe.com.security.scholarship.dto.response.ConvocatoriaAbiertaResponse;
import pe.com.security.scholarship.dto.response.DetalleConvocatoriaResponse;
import pe.com.security.scholarship.dto.response.HistorialConvocatoriaResponse;
import pe.com.security.scholarship.dto.response.RegisteredConvocatoriaResponse;
import pe.com.security.scholarship.domain.entity.Convocatoria;
import pe.com.security.scholarship.domain.entity.Empleado;
import pe.com.security.scholarship.domain.enums.EstadoConvocatoria;

import java.util.List;

public class ConvocatoriaMapper {

  public static Convocatoria buildConvocatoria(RegisterConvocatoriaRequest request, Empleado empleado) {
    return Convocatoria.builder()
            .mes(request.getMes())
            .fechaInicio(request.getFechaInicio())
            .fechaFin(request.getFechaFin())
            .estado(EstadoConvocatoria.PROGRAMADO)
            .cantidadVacantes(request.getCantidadVacantes())
            .createdBy(empleado)
            .build();
  }

  public static RegisteredConvocatoriaResponse mapRegisteredConvocatoria(Convocatoria convocatoria,
                                                                         AuditEmpleadoResponse auditEmpleadoResponse) {
    return RegisteredConvocatoriaResponse.builder()
            .id(convocatoria.getId())
            .mes(convocatoria.getMes())
            .fechaInicio(convocatoria.getFechaInicio())
            .fechaFin(convocatoria.getFechaFin())
            .estado(convocatoria.getEstado())
            .cantidadVacantes(convocatoria.getCantidadVacantes())
            .createdAt(convocatoria.getCreatedAt())
            .createdBy(auditEmpleadoResponse)
            .build();
  }

  public static ConvocatoriaAbiertaResponse mapConvocatoriaAbierta(Convocatoria convocatoria) {
    return ConvocatoriaAbiertaResponse.builder()
            .id(convocatoria.getId())
            .mes(convocatoria.getMes())
            .fechaInicio(convocatoria.getFechaInicio())
            .fechaFin(convocatoria.getFechaFin())
            .cantidadVacantes(convocatoria.getCantidadVacantes())
            .build();
  }

  public static HistorialConvocatoriaResponse mapHistorialConvocatoria(Convocatoria convocatoria) {
    return HistorialConvocatoriaResponse.builder()
            .id(convocatoria.getId())
            .mes(convocatoria.getMes())
            .fechaInicio(convocatoria.getFechaInicio())
            .fechaFin(convocatoria.getFechaFin())
            .estado(convocatoria.getEstado())
            .createdAt(convocatoria.getCreatedAt())
            .updatedAt(convocatoria.getUpdatedAt())
            .build();
  }

  public static DetalleConvocatoriaResponse mapDetalleConvocatoria(Convocatoria convocatoria, AuditEmpleadoResponse auditEmpleado, int cantPostulantes,
                                                                   TasasConvocatoriaProjection tasas, List<RankingProjection> rankingSocioeconomico,
                                                                   List<RankingProjection> rankingCiclo, List<RankingProjection> rankingCarrera) {
    return DetalleConvocatoriaResponse.builder()
            .datosGeneralesConvocatoria(mapHistorialConvocatoria(convocatoria))
            .createdBy(auditEmpleado)
            .cantidadVacantes(convocatoria.getCantidadVacantes())
            .cantidadPostulantes(cantPostulantes)
            .tasaAceptacion(String.format("%.0f%%", tasas.getTasaAceptacion() * 100))
            .tasaMatriculados(String.format("%.0f%%", tasas.getTasaMatriculados() * 100))
            .rankingSocioeconomico(rankingSocioeconomico)
            .rankingCiclos(rankingCiclo)
            .rankingCarreras(rankingCarrera)
            .build();
  }
}
