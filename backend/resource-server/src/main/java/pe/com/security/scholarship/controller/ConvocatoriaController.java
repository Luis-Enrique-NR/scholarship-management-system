package pe.com.security.scholarship.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.security.scholarship.dto.request.RegisterConvocatoriaRequest;
import pe.com.security.scholarship.dto.response.RegisteredConvocatoriaResponse;
import pe.com.security.scholarship.service.ConvocatoriaService;
import pe.com.security.scholarship.util.ApiResponse;

@RestController
@RequestMapping("/api/convocatorias")
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

  //public ResponseEntity<ApiResponse>
}
