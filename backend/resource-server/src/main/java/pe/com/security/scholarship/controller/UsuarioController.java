package pe.com.security.scholarship.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.security.scholarship.dto.request.CreateConvocatoriaRequest;
import pe.com.security.scholarship.dto.request.CreatePostulacionRequest;
import pe.com.security.scholarship.entity.Convocatoria;
import pe.com.security.scholarship.entity.Matricula;
import pe.com.security.scholarship.entity.Postulacion;
import pe.com.security.scholarship.util.ApiResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/usuario")
@RequiredArgsConstructor
public class UsuarioController {

  /*
  // Mostrar convocatoria: Todos

  @GetMapping("/convocatorias")
  public ResponseEntity<ApiResponse<List<Convocatoria>>> obtenerConvocatorias() {
    List<Convocatoria> listaConvocatoria = new ArrayList<>();
    listaConvocatoria.add(new Convocatoria(1L, "Mayo", LocalDate.now(), LocalDate.now(), "Finalizado"));
    listaConvocatoria.add(new Convocatoria(2L, "Junio", LocalDate.now(), LocalDate.now(), "Finalizado"));
    listaConvocatoria.add(new Convocatoria(1L, "Julio", LocalDate.now(), LocalDate.now(), "Aperturado"));

    return ResponseEntity.ok(new ApiResponse<>("Consulta exitosa", "200", listaConvocatoria));
  }

  // Abrir convocatoria: Secretaria UECPS

  @PostMapping("/convocatorias")
  @PreAuthorize("hasRole('SOCIAL_OUTREACH_SECRETARY') or hasRole('SOCIAL_OUTREACH_MANAGER')")
  public ResponseEntity<ApiResponse<Convocatoria>> registrarConvocatoria(
          @RequestBody CreateConvocatoriaRequest dto
  ) {
    Convocatoria convocatoria = new Convocatoria(1L, dto.getMes(), dto.getFechaInicio(), dto.getFechaFin(), "Aperturado");

    return ResponseEntity.ok(new ApiResponse<>("Registro exitoso", "200", convocatoria));
  }

   */

  // Postular convocatoria: Estudiante

  @PostMapping("/postulaciones")
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<ApiResponse<Postulacion>> postularConvocatoria(
          @RequestBody CreatePostulacionRequest dto
  ) {
    //Convocatoria convocatoria  = new Convocatoria(1L, "Junio", LocalDate.parse("2026-05-26"),
      //      LocalDate.parse("2026-05-30"), "Aperturado");

    Postulacion postulacion = new Postulacion(1L, dto.getCursoOpcion(), LocalDateTime.now());//,convocatoria);

    return ResponseEntity.ok(new ApiResponse<>("Registro exitoso", "200", postulacion));
  }

  // Cerrar convocatoria: Jefe UECPS

  @PatchMapping("/convocatorias/{idConvocatoria}/{estado}")
  @PreAuthorize("hasRole('SOCIAL_OUTREACH_MANAGER')")
  public ResponseEntity<ApiResponse<String>> cerrarConvocatoria(
          @PathVariable Long idConvocatoria,
          @PathVariable String estado
  ) {
    //Convocatoria convocatoria  = new Convocatoria(idConvocatoria, "Junio", LocalDate.parse("2026-05-26"),
     //       LocalDate.parse("2026-05-30"), "Aperturado");

    //convocatoria.setEstado(estado);

    return ResponseEntity.ok(new ApiResponse<>("Actualización exitosa", "200", null));// convocatoria));
  }

  // Subir nota: Secretaria Sistemas UNI

  @PatchMapping("/matricula/{nota}")
  @PreAuthorize("hasRole('TRAINING_CENTER_SECRETARY')")
  public ResponseEntity<ApiResponse<Matricula>> subirNota(
           @PathVariable Double nota
  ) {
    Matricula matricula = new Matricula(1L, 0.0);
    matricula.setNota(nota);

    return ResponseEntity.ok(new ApiResponse<>("Actualización de notas exitosa", "200", matricula));
  }
}
