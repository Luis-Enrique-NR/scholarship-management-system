package pe.com.security.scholarship.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.security.scholarship.dto.request.RegisterConvocatoriaRequest;
import pe.com.security.scholarship.dto.request.UpdateEstadoConvocatoriaRequest;
import pe.com.security.scholarship.dto.response.ConvocatoriaAbiertaResponse;
import pe.com.security.scholarship.dto.response.DetalleConvocatoriaResponse;
import pe.com.security.scholarship.dto.response.HistorialConvocatoriaResponse;
import pe.com.security.scholarship.dto.response.RegisteredConvocatoriaResponse;
import pe.com.security.scholarship.service.ConvocatoriaService;
import pe.com.security.scholarship.util.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/convocatorias")
@RequiredArgsConstructor
@Tag(name = "Convocatorias", description = "Endpoints para el registro, consulta y gestión de convocatorias")
public class ConvocatoriaController {

  private final ConvocatoriaService convocatoriaService;

  @PostMapping
  @PreAuthorize("hasRole('SOCIAL_OUTREACH_SECRETARY') or hasRole('SOCIAL_OUTREACH_MANAGER')")
  @Operation(summary = "Registro de convocatoria", description = "Programar el inicio y fin de una convocatoria")
  public ResponseEntity<ApiResponse<RegisteredConvocatoriaResponse>> registerConvocatoria(
          @Valid @RequestBody RegisterConvocatoriaRequest request
  ) {
    RegisteredConvocatoriaResponse response = convocatoriaService.registerConvocatoria(request);
    return ResponseEntity.ok(new ApiResponse<>("Registro exitoso", "200", response));
  }

  @GetMapping("/activa")
  @Operation(summary = "Consultar convocatoria activa", description = "Consultar la convocatoria aperturada")
  public ResponseEntity<ApiResponse<ConvocatoriaAbiertaResponse>> getConvocatoriaAbierta() {
    ConvocatoriaAbiertaResponse response = convocatoriaService.getConvocatoriaAbierta();
    return ResponseEntity.ok(new ApiResponse<>("Consulta exitosa", "200", response));
  }

  @GetMapping("/historial")
  @PreAuthorize("hasRole('SOCIAL_OUTREACH_SECRETARY') or hasRole('SOCIAL_OUTREACH_MANAGER')")
  @Operation(summary = "Consultar historial de convocatorias", description = "Obtener una lista de convocatorias pasadas por año")
  public ResponseEntity<ApiResponse<List<HistorialConvocatoriaResponse>>> getHistorialConvocatorias(
          @RequestParam Integer year
  ) {
    List<HistorialConvocatoriaResponse> responses = convocatoriaService.getHistorialConvocatorias(year);
    return ResponseEntity.ok(new ApiResponse<>("Consulta exitosa", "200", responses));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('SOCIAL_OUTREACH_SECRETARY') or hasRole('SOCIAL_OUTREACH_MANAGER')")
  @Operation(summary = "Consultar detalles de convocatoria", description = "Obtener información detallada de una convocatoria para asegurar la trazabilidad")
  public ResponseEntity<ApiResponse<DetalleConvocatoriaResponse>> getDetallesConvocatoria(
          @PathVariable Integer id
  ) {
    DetalleConvocatoriaResponse response = convocatoriaService.getDetalleConvocatoria(id);
    return ResponseEntity.ok(new ApiResponse<>("Consulta exitosa", "200", response));
  }

  @PatchMapping("/estado")
  @PreAuthorize("hasRole('SOCIAL_OUTREACH_MANAGER')")
  @Operation(summary = "Actualizar estado de convocatoria", description = "Aprobar o rechazar los resultados de la convocatoria")
  public ResponseEntity<ApiResponse<DetalleConvocatoriaResponse>> actualizarEstadoConvocatoria(
          @Valid @RequestBody UpdateEstadoConvocatoriaRequest request
  ) {
    DetalleConvocatoriaResponse response = convocatoriaService.actualizarEstadoConvocatoria(request);
    return ResponseEntity.ok(new ApiResponse<>("Actualización exitosa", "200", response));
  }
}
