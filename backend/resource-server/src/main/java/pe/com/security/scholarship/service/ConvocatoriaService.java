package pe.com.security.scholarship.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.security.scholarship.dto.request.RegisterConvocatoriaRequest;
import pe.com.security.scholarship.dto.response.AuditEmpleadoResponse;
import pe.com.security.scholarship.dto.response.RegisteredConvocatoriaResponse;
import pe.com.security.scholarship.entity.Convocatoria;
import pe.com.security.scholarship.entity.Empleado;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.mapper.ConvocatoriaMapper;
import pe.com.security.scholarship.repository.ConvocatoriaRepository;
import pe.com.security.scholarship.repository.EmpleadoRepository;
import pe.com.security.scholarship.util.SecurityUtils;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConvocatoriaService {

  private final ConvocatoriaRepository convocatoriaRepository;
  private final EmpleadoRepository empleadoRepository;
  private final EmpleadoService empleadoService;

  @Transactional
  public RegisteredConvocatoriaResponse registerConvocatoria(RegisterConvocatoriaRequest request) {
    UUID idUsuario = SecurityUtils.getCurrentUserId();
    System.out.println("ID usuario: "+idUsuario);
    Empleado empleado = empleadoRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NotFoundException("No se encontró empleado con el user id del payload"));

    // Debe validarse el rol del usuario?

    Convocatoria convocatoria = convocatoriaRepository.save(ConvocatoriaMapper.buildConvocatoria(request, empleado));
    AuditEmpleadoResponse auditEmpleadoResponse = empleadoService.obtenerAuditoriaActual();
    return ConvocatoriaMapper.mapRegisteredConvocatoria(convocatoria, auditEmpleadoResponse);
  }

  @Scheduled(cron = "0 0 0 * * *")
  public void revisarAperturaDeConvocatorias() {
    int aperturadas = convocatoriaRepository.aperturarConvocatoriasVigentes(LocalDate.now());

    if (aperturadas > 0) {
      System.out.println("Cron ejecutado: Se aperturaron " + aperturadas + " convocatorias.");
    }
  }

  @Scheduled(cron = "0 0 0 * * *")
  public void revisarCierreDeConvocatorias() {
    int cerradas = convocatoriaRepository.cerrarConvocatoriasExpiradas(LocalDate.now());

    if (cerradas > 0) {
      System.out.println("Cron ejecutado: Se cerraron " + cerradas + " convocatorias.");
    }
  }
}
