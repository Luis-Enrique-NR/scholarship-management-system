package pe.com.security.scholarship.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.com.security.scholarship.dto.ProcesamientoResult;
import pe.com.security.scholarship.service.PromedioPonderadoService;
import pe.com.security.scholarship.util.ApiResponse;
import pe.com.security.scholarship.util.FileUtils;

@RestController
@RequestMapping("/api/v1/promedios")
@RequiredArgsConstructor
@Tag(name = "Promedios Ponderados", description = "Endpoints para la carga masiva y gestión de promedios académicos")
public class PromedioPonderadoController {

  private final PromedioPonderadoService promedioService;

  @PostMapping(value = "/upload/{periodo}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('TRAINING_CENTER_SECRETARY')")
  @Operation(summary = "Carga masiva de promedios ponderados", description = "Cargar un archivo CSV con las columnas: codigo, ciclo, promedio")
  public ResponseEntity<ApiResponse<ProcesamientoResult>> uploadPromedios(
          @RequestParam("file") MultipartFile file,
          @PathVariable String periodo
  ) {
    FileUtils.validarCsv(file);

    ProcesamientoResult response = promedioService.procesarCargaPromedios(file, periodo);
    return ResponseEntity.ok(new ApiResponse<>("Procesamiento finalizado", "200", response));
  }
}
