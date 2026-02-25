package pe.com.security.scholarship.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.com.security.scholarship.domain.entity.Empleado;
import pe.com.security.scholarship.domain.entity.Estudiante;
import pe.com.security.scholarship.domain.entity.PeriodoAcademico;
import pe.com.security.scholarship.dto.ProcesamientoResult;
import pe.com.security.scholarship.dto.request.PromedioCsvRequest;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.mapper.PromedioPonderadoMapper;
import pe.com.security.scholarship.repository.EmpleadoRepository;
import pe.com.security.scholarship.repository.EstudianteRepository;
import pe.com.security.scholarship.repository.PeriodoAcademicoRepository;
import pe.com.security.scholarship.repository.PromedioPonderadoRepository;
import pe.com.security.scholarship.util.CargaMasivaHelper;
import pe.com.security.scholarship.util.SecurityUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromedioPonderadoService {
  private final CargaMasivaHelper cargaMasivaHelper;
  private final EstudianteRepository estudianteRepository;
  private final EmpleadoRepository empleadoRepository;
  private final PromedioPonderadoRepository promedioRepository;
  private final PeriodoAcademicoRepository periodoAcademicoRepository;

  @Transactional
  public ProcesamientoResult procesarCargaPromedios(MultipartFile file, String periodo) {
    UUID idUsuario = SecurityUtils.getCurrentUserId();
    Empleado empleado = empleadoRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NotFoundException("Empleado no encontrado"));

    PeriodoAcademico periodoAcademico = periodoAcademicoRepository.findByPeriodo(periodo)
            .orElseThrow(() -> new NotFoundException("Periodo académico no registrado"));

    return cargaMasivaHelper.procesar(file, PromedioCsvRequest.class, fila -> {
      // 1. Validar existencia del estudiante
      Estudiante estudiante = estudianteRepository.findByCodigo(fila.getCodigo())
              .orElseThrow(() -> new NotFoundException("Estudiante no encontrado"));

      // 2. Validaciones de negocio específicas
      if (fila.getPromedio() < 0 || fila.getPromedio() > 20) {
        throw new BadRequestException("El promedio debe estar entre 0 y 20");
      }

      if (fila.getCiclo() < 1 || fila.getCiclo() > 12) {
        throw new BadRequestException("Ciclo académico inválido");
      }

      // 3. Guardado o Actualización
      promedioRepository.save(PromedioPonderadoMapper
              .buildPromedioPonderado(estudiante, periodoAcademico, empleado, fila.getCiclo(), fila.getPromedio()));
    });
  }
}
