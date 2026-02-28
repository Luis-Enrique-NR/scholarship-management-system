package pe.com.security.scholarship.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.security.scholarship.dto.request.RegisterPostulacionRequest;
import pe.com.security.scholarship.dto.response.ConsultaPostulacionResponse;
import pe.com.security.scholarship.dto.response.DetallePostulanteResponse;
import pe.com.security.scholarship.dto.response.HistorialPostulacionResponse;
import pe.com.security.scholarship.dto.response.RegisteredPostulacionResponse;
import pe.com.security.scholarship.service.PostulacionService;
import pe.com.security.scholarship.util.ApiResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/postulaciones")
@RequiredArgsConstructor
@Tag(name = "Postulaciones", description = "Endpoints para el registro y seguimiento de postulaciones")
public class PostulacionController {

  private final PostulacionService postulacionService;

  @PostMapping
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Registro de postulación", description = "Crear una postulación a partir de la selección de cursos")
  public ResponseEntity<ApiResponse<RegisteredPostulacionResponse>> registerPostulacion(
          @Valid @RequestBody RegisterPostulacionRequest request
  ) {
    RegisteredPostulacionResponse response = postulacionService.registerPostulacion(request);
    return ResponseEntity.ok(new ApiResponse<>("Registro exitoso", "200", response));
  }

  @GetMapping("/{idPostulacion}")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Consulta de postulación", description = "Consultar estado actual de la postulación")
  public ResponseEntity<ApiResponse<ConsultaPostulacionResponse>> getDetallePostulacion(
          @PathVariable Integer idPostulacion
  ) {
    ConsultaPostulacionResponse response = postulacionService.getDetallePostulacion(idPostulacion);
    return ResponseEntity.ok(new ApiResponse<>("Consulta exitosa", "200", response));
  }

  @GetMapping("/historial")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Consulta de postulación", description = "Consultar estado actual de la postulación")
  public ResponseEntity<ApiResponse<List<HistorialPostulacionResponse>>> getHistorialPostulacion(
          @RequestParam Integer year
  ) {
    List<HistorialPostulacionResponse> response = postulacionService.getHistorialPostulacion(year);
    return ResponseEntity.ok(new ApiResponse<>("Consulta exitosa", "200", response));
  }

  @GetMapping("/{id}/{year}")
  @PreAuthorize("hasRole('SOCIAL_OUTREACH_SECRETARY') or hasRole('SOCIAL_OUTREACH_MANAGER')")
  public ResponseEntity<ApiResponse<DetallePostulanteResponse>> getPostulacionesEstudiante(
          @PathVariable UUID id,
          @PathVariable Integer year
  ) {
    DetallePostulanteResponse response = postulacionService.getPostulacionesEstudiante(id, year);
    return ResponseEntity.ok(new ApiResponse<>("Consulta exitosa", "200", response));
  }
}
