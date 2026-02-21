package pe.com.security.scholarship.mapper;

import pe.com.security.scholarship.dto.request.RegisterConvocatoriaRequest;
import pe.com.security.scholarship.dto.response.AuditEmpleadoResponse;
import pe.com.security.scholarship.dto.response.ConvocatoriaAbiertaResponse;
import pe.com.security.scholarship.dto.response.HistorialConvocatoriaResponse;
import pe.com.security.scholarship.dto.response.RegisteredConvocatoriaResponse;
import pe.com.security.scholarship.entity.Convocatoria;
import pe.com.security.scholarship.entity.Empleado;
import pe.com.security.scholarship.entity.enums.EstadoConvocatoria;

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
            .mes(convocatoria.getMes())
            .fechaInicio(convocatoria.getFechaInicio())
            .fechaFin(convocatoria.getFechaFin())
            .cantidadVacantes(convocatoria.getCantidadVacantes())
            .build();
  }

  public static HistorialConvocatoriaResponse mapHistorialConvocatoria(Convocatoria convocatoria) {
    return HistorialConvocatoriaResponse.builder()
            .mes(convocatoria.getMes())
            .fechaInicio(convocatoria.getFechaInicio())
            .fechaFin(convocatoria.getFechaFin())
            .estado(convocatoria.getEstado())
            .createdAt(convocatoria.getCreatedAt())
            .updatedAt(convocatoria.getUpdatedAt())
            .build();
  }
}
