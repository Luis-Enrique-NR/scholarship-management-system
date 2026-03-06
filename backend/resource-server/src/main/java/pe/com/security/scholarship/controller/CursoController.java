package pe.com.security.scholarship.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.security.scholarship.dto.request.RegisterCursoRequest;
import pe.com.security.scholarship.dto.response.RegisteredCursoResponse;
import pe.com.security.scholarship.service.CursoService;
import pe.com.security.scholarship.util.ApiResponse;

@RestController
@RequestMapping("/api/v1/cursos")
@RequiredArgsConstructor
@Tag(name = "Cursos", description = "Endpoints para la gestión de cursos de especialización")
public class CursoController {

  private final CursoService cursoService;

  @PostMapping
  @PreAuthorize("hasRole('TRAINING_CENTER_SECRETARY')")
  @Operation(summary = "Creación de un nuevo curso con secciones y horarios",
          description = "Realizar el registro integral de un curso de especialización, incluyendo sus secciones " +
                  "programadas y los horarios correspondientes")
  public ResponseEntity<ApiResponse<RegisteredCursoResponse>> register(
          @Valid @RequestBody RegisterCursoRequest request
  ) {
    RegisteredCursoResponse response = cursoService.register(request);
    return ResponseEntity.ok(new ApiResponse<>("Registro exitoso", "200", response));
  }
}
