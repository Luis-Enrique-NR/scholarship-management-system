package pe.com.security.scholarship.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.com.security.scholarship.domain.entity.Empleado;
import pe.com.security.scholarship.domain.entity.Estudiante;
import pe.com.security.scholarship.domain.enums.NivelSocioeconomico;
import pe.com.security.scholarship.dto.ProcesamientoResult;
import pe.com.security.scholarship.dto.request.EvaluacionCsvRequest;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.mapper.EvaluacionSocioeconomicaMapper;
import pe.com.security.scholarship.repository.EmpleadoRepository;
import pe.com.security.scholarship.repository.EstudianteRepository;
import pe.com.security.scholarship.repository.EvaluacionSocioeconomicaRepository;
import pe.com.security.scholarship.util.CargaMasivaHelper;
import pe.com.security.scholarship.util.SecurityUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EvaluacionSocioeconomicaService {

  private final CargaMasivaHelper cargaMasivaHelper;
  private final EstudianteRepository estudianteRepository;
  private final EmpleadoRepository empleadoRepository;
  private final EvaluacionSocioeconomicaRepository evaluacionRepository;

  @Transactional
  public ProcesamientoResult procesarCargaMasiva(MultipartFile file) {
    UUID idUsuario = SecurityUtils.getCurrentUserId();
    Empleado empleado = empleadoRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NotFoundException("Empleado no encontrado"));

    return cargaMasivaHelper.procesar(file, EvaluacionCsvRequest.class, fila -> {
      Estudiante estudiante = estudianteRepository.findByCodigo(fila.getCodigo())
              .orElseThrow(() -> new NotFoundException("Estudiante no encontrado"));

      NivelSocioeconomico nivel;
      try {
        nivel = NivelSocioeconomico.valueOf(fila.getNivel().toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new BadRequestException("Nivel socioeconómico '" + fila.getNivel() + "' no es válido");
      }

      evaluacionRepository.save(EvaluacionSocioeconomicaMapper
              .buildEvaluacionSocioeconomica(estudiante, fila.getFechaEvaluacion(), nivel, empleado));
    });
  }
}
