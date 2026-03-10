package pe.com.security.scholarship.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.security.scholarship.dto.request.RegisterCursoRequest;
import pe.com.security.scholarship.dto.response.OverviewCursoResponse;
import pe.com.security.scholarship.dto.response.RegisteredCursoResponse;
import pe.com.security.scholarship.service.CursoService;
import pe.com.security.scholarship.util.ApiResponse;

import java.util.List;

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

  @GetMapping
  @Operation(summary = "Consulta del catálogo completo de cursos",
          description = "Obtener la lista completa de cursos de especialización")
  public ResponseEntity<ApiResponse<Page<OverviewCursoResponse>>> getCatalogo(
          @ParameterObject @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable
  ) {
    Page<OverviewCursoResponse> response = cursoService.getCatalogo(pageable);
    return ResponseEntity.ok(new ApiResponse<>("Consulta exitosa", "200", response));
  }

  @GetMapping("/horarios")
  @Operation(summary = "Catálogo público de horarios y secciones vigentes",
          description = "Consultar la oferta académica activa, incluyendo detalles de secciones, fechas de inicio y horarios programados")
  public ResponseEntity<ApiResponse<Page<OverviewCursoResponse>>> getHorarios(
          @ParameterObject @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable
  ) {
    Page<OverviewCursoResponse> response = cursoService.getHorarios(pageable);
    return ResponseEntity.ok(new ApiResponse<>("Consulta exitosa", "200", response));
  }

  @GetMapping("/beca")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Oferta académica personalizada para estudiante becado",
          description = "Obtener exclusivamente los cursos y secciones disponibles para un estudiante con beca vigente")
  public ResponseEntity<ApiResponse<List<OverviewCursoResponse>>> getCursosDisponibles() {
    List<OverviewCursoResponse> response = cursoService.getOfertaDisponiblePorBeca();
    return ResponseEntity.ok(new ApiResponse<>("Consulta exitosa", "200", response));
  }
}
