package pe.com.security.scholarship.mapper;

import pe.com.security.scholarship.dto.response.UpdatedVacantesSeccionResponse;

public class SeccionMapper {

  public static UpdatedVacantesSeccionResponse mapUpdatedVacantes(int cantidadNuevosMatriculados, int totalMatriculados) {
    return UpdatedVacantesSeccionResponse.builder()
            .cantidadNuevosMatriculados(cantidadNuevosMatriculados)
            .totalMatriculados(totalMatriculados)
            .build();
  }
}
