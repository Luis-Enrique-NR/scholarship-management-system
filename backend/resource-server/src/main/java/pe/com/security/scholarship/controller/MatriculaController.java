package pe.com.security.scholarship.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.security.scholarship.dto.request.SubmitMatriculaRequest;
import pe.com.security.scholarship.dto.response.IntencionMatriculaResponse;
import pe.com.security.scholarship.dto.response.RegisteredMatriculaResponse;
import pe.com.security.scholarship.service.MatriculaService;
import pe.com.security.scholarship.util.ApiResponse;

@RestController
@RequestMapping("/api/v1/matriculas")
@RequiredArgsConstructor
@Tag(name = "Matriculas", description = "Endpoints para registrar intenciones de matrícula y realizar su seguimiento")
public class MatriculaController {

  private final MatriculaService matriculaService;

  @PostMapping
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Registro de intención de matrícula", description = "Ingresar la intención de matrícula especificando el ID de la sección")
  public ResponseEntity<ApiResponse<RegisteredMatriculaResponse>> submitEnrollmentIntention(
          @Valid @RequestBody SubmitMatriculaRequest request
  ) {
    RegisteredMatriculaResponse response = matriculaService.submitEnrollmentIntention(request);
    return ResponseEntity.ok(new ApiResponse<>("Registro exitoso", "200", response));
  }

  @GetMapping
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Consulta del estado de intención de matrícula",
          description = "Obtener información detallada de la intención de matrícula para realizar seguimiento")
  public ResponseEntity<ApiResponse<IntencionMatriculaResponse>> getIntencionMatricula() {
    IntencionMatriculaResponse response = matriculaService.getIntencionMatricula();
    return ResponseEntity.ok(new ApiResponse<>("Registro exitoso", "200", response));
  }
}
