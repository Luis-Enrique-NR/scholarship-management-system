package pe.com.security.scholarship.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.security.scholarship.dto.request.RegisterSeccionRequest;
import pe.com.security.scholarship.dto.request.UpdateVacantesSeccionRequest;
import pe.com.security.scholarship.dto.response.RegisteredSeccionResponse;
import pe.com.security.scholarship.dto.response.UpdatedVacantesSeccionResponse;
import pe.com.security.scholarship.service.SeccionService;
import pe.com.security.scholarship.util.ApiResponse;

@RestController
@RequestMapping("/api/v1/secciones")
@RequiredArgsConstructor
@Tag(name = "Secciones", description = "Endpoints para habilitar secciones de cursos y gestionar sus cambios")
public class SeccionController {

  private final SeccionService seccionService;

  @PostMapping("/{idCurso}")
  @PreAuthorize("hasRole('TRAINING_CENTER_SECRETARY')")
  @Operation(summary = "Registro de una sección",
          description = "Registrar una nueva sección para un determinado curso con validaciones de horarios")
  public ResponseEntity<ApiResponse<RegisteredSeccionResponse>> register(
          @Valid @RequestBody RegisterSeccionRequest request,
          @PathVariable Integer idCurso
  ) {
    RegisteredSeccionResponse response = seccionService.register(request, idCurso);
    return ResponseEntity.ok(new ApiResponse<>("Registro exitoso", "200", response));
  }

  @PatchMapping("/vacantes")
  @PreAuthorize("hasRole('TRAINING_CENTER_SECRETARY')")
  @Operation(summary = "Actualización de vacantes disponibles por sección",
          description = "Actualizar la cantidad de vacantes disponibles en una sección específica y matricular nuevos estudiantes")
  public ResponseEntity<ApiResponse<UpdatedVacantesSeccionResponse>> updateVacantesSeccion(
          @Valid @RequestBody UpdateVacantesSeccionRequest request
  ) {
    UpdatedVacantesSeccionResponse response = seccionService.updateVacantes(request);
    return ResponseEntity.ok(new ApiResponse<>("Actualización exitosa", "200", response));
  }
}
