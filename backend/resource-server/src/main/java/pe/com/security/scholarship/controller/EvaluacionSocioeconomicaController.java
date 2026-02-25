package pe.com.security.scholarship.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.com.security.scholarship.dto.ProcesamientoResult;
import pe.com.security.scholarship.service.EvaluacionSocioeconomicaService;
import pe.com.security.scholarship.util.ApiResponse;
import pe.com.security.scholarship.util.FileUtils;

@RestController
@RequestMapping("/api/v1/evaluaciones")
@RequiredArgsConstructor
@Tag(name = "Evaluaciones Socioeconómicas", description = "Endpoints para la carga masiva y gestión de evaluaciones socioeconómicas")
public class EvaluacionSocioeconomicaController {

  private final EvaluacionSocioeconomicaService evaluacionService;

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('SOCIAL_OUTREACH_SECRETARY')")
  @Operation(summary = "Carga masiva de evaluaciones", description = "Cargar un archivo CSV con columnas: codigo, nivel, fecha_evaluacion")
  public ResponseEntity<ApiResponse<ProcesamientoResult>> uploadEvaluaciones(
          @RequestParam("file") MultipartFile file
  ) {
    FileUtils.validarCsv(file);

    ProcesamientoResult response = evaluacionService.procesarCargaMasiva(file);
    return ResponseEntity.ok(new ApiResponse<>("Procesamiento finalizado", "200", response));
  }
}
