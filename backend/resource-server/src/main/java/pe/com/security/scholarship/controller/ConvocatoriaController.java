package pe.com.security.scholarship.controller;

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
import pe.com.security.scholarship.dto.request.RegisterConvocatoriaRequest;
import pe.com.security.scholarship.dto.response.ConvocatoriaAbiertaResponse;
import pe.com.security.scholarship.dto.response.HistorialConvocatoriaResponse;
import pe.com.security.scholarship.dto.response.RegisteredConvocatoriaResponse;
import pe.com.security.scholarship.service.ConvocatoriaService;
import pe.com.security.scholarship.util.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/convocatorias")
@RequiredArgsConstructor
public class ConvocatoriaController {

  private final ConvocatoriaService convocatoriaService;

  @PostMapping
  @PreAuthorize("hasRole('SOCIAL_OUTREACH_SECRETARY') or hasRole('SOCIAL_OUTREACH_MANAGER')")
  public ResponseEntity<ApiResponse<RegisteredConvocatoriaResponse>> registerConvocatoria(
          @Valid @RequestBody RegisterConvocatoriaRequest request
  ) {
    RegisteredConvocatoriaResponse response = convocatoriaService.registerConvocatoria(request);
    return ResponseEntity.ok(new ApiResponse<>("Registro exitoso", "200", response));
  }

  @GetMapping("/activa")
  public ResponseEntity<ApiResponse<ConvocatoriaAbiertaResponse>> getConvocatoriaAbierta() {
    ConvocatoriaAbiertaResponse response = convocatoriaService.getConvocatoriaAbierta();
    return ResponseEntity.ok(new ApiResponse<>("Consulta exitosa", "200", response));
  }

  @GetMapping("/historial")
  @PreAuthorize("hasRole('SOCIAL_OUTREACH_SECRETARY') or hasRole('SOCIAL_OUTREACH_MANAGER')")
  public ResponseEntity<ApiResponse<List<HistorialConvocatoriaResponse>>> getHistorialConvocatorias(
          @RequestParam Integer year
  ) {
    List<HistorialConvocatoriaResponse> responses = convocatoriaService.getHistorialConvocatorias(year);
    return ResponseEntity.ok(new ApiResponse<>("Consulta exitosa", "200", responses));
  }
}
