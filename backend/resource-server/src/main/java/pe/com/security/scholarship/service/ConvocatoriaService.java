package pe.com.security.scholarship.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;
import pe.com.security.scholarship.dto.projection.RankingProjection;
import pe.com.security.scholarship.dto.projection.TasasConvocatoriaProjection;
import pe.com.security.scholarship.dto.request.RegisterConvocatoriaRequest;
import pe.com.security.scholarship.dto.response.AuditEmpleadoResponse;
import pe.com.security.scholarship.dto.response.ConvocatoriaAbiertaResponse;
import pe.com.security.scholarship.dto.response.DetalleConvocatoriaResponse;
import pe.com.security.scholarship.dto.response.HistorialConvocatoriaResponse;
import pe.com.security.scholarship.dto.response.RegisteredConvocatoriaResponse;
import pe.com.security.scholarship.domain.entity.Convocatoria;
import pe.com.security.scholarship.domain.entity.Empleado;
import pe.com.security.scholarship.domain.enums.EstadoConvocatoria;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.ConflictException;
import pe.com.security.scholarship.exception.InternalServerErrorException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.mapper.ConvocatoriaMapper;
import pe.com.security.scholarship.repository.ConvocatoriaRepository;
import pe.com.security.scholarship.repository.EmpleadoRepository;
import pe.com.security.scholarship.util.SecurityUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConvocatoriaService {

  private final ConvocatoriaRepository convocatoriaRepository;
  private final EmpleadoRepository empleadoRepository;
  private final EmpleadoService empleadoService;
  private final EvaluacionPostulanteService evaluacionPostulanteService;

  @Transactional
  public RegisteredConvocatoriaResponse registerConvocatoria(RegisterConvocatoriaRequest request) {
    if (!request.getFechaFin().isAfter(request.getFechaInicio())) {
      throw new BadRequestException("La fecha de finalización debe ser posterior a la fecha de inicio");
    }

    UUID idUsuario = SecurityUtils.getCurrentUserId();
    Empleado empleado = empleadoRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NotFoundException("No se encontró empleado con el user id del payload"));

    List<HistorialConvocatoriaResponse> convocatorias = getHistorialConvocatorias(request.getFechaInicio().getYear());

    // No pueden haber dos convocatorias con el mismo mes en el mismo año (a menos que haya sido rechazada)
    boolean mesRepetido = convocatorias.stream()
            .anyMatch(conv -> conv.getMes().equals(request.getMes())
                    && conv.getEstado() != EstadoConvocatoria.RECHAZADO);

    if (mesRepetido) {
      throw new BadRequestException(String.format("Ya existe una convocatoria registrada para el mes de %s",
              request.getMes()));
    }

    // No pueden haber dos periodos cruzados de convocatorias
    boolean periodoCruzado = convocatorias.stream()
            .anyMatch(convoc ->
                    convoc.getEstado() != EstadoConvocatoria.RECHAZADO &&
                            request.getFechaInicio().isBefore(convoc.getFechaFin()) &&
                            request.getFechaFin().isAfter(convoc.getFechaInicio()));

    if (periodoCruzado) {
      throw new BadRequestException("Las fechas seleccionadas presentan un conflicto con otra convocatoria programada");
    }

    Convocatoria convocatoria = convocatoriaRepository.save(ConvocatoriaMapper.buildConvocatoria(request, empleado));
    AuditEmpleadoResponse auditEmpleadoResponse = empleadoService.obtenerAuditoriaActual();
    return ConvocatoriaMapper.mapRegisteredConvocatoria(convocatoria, auditEmpleadoResponse);
  }

  public ConvocatoriaAbiertaResponse getConvocatoriaAbierta() {
    try {
      Convocatoria convocatoria = convocatoriaRepository.findConvocatoriaAperturada()
              .orElseThrow(() -> new NotFoundException("No se encontró ninguna convocatoria abierta"));

      return ConvocatoriaMapper.mapConvocatoriaAbierta(convocatoria);
    } catch (IncorrectResultSizeDataAccessException e) {
      throw new ConflictException("Existe más de una convocatoria abierta");
    }
  }

  public List<HistorialConvocatoriaResponse> getHistorialConvocatorias(Integer year) {
    List<Convocatoria> convocatorias = convocatoriaRepository.findByYear(year);
    if (convocatorias == null || convocatorias.isEmpty()) {
      return Collections.emptyList();
    }

    return convocatorias.stream()
            .map(ConvocatoriaMapper::mapHistorialConvocatoria)
            .toList();
  }

  public DetalleConvocatoriaResponse getDetalleConvocatoria(Integer id) {
    Convocatoria convocatoria = convocatoriaRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("No se encontró convocatoria con el id especificado"));
    AuditEmpleadoResponse auditEmpleadoResponse = empleadoService.obtenerAuditoriaActual();
    int cantidadPostulantes = convocatoriaRepository.getCantidadPostulantes(id);
    TasasConvocatoriaProjection tasas = convocatoriaRepository.getTasasGenerales(id);
    if (tasas == null) throw new InternalServerErrorException("No se pudieron calcular las tasas de la convocatoria");

    List<RankingProjection> rankingSocioeconomico = convocatoriaRepository.getRankingSocioeconomico(id);
    List<RankingProjection> rankingCiclo = convocatoriaRepository.getRankingCiclo(id);
    List<RankingProjection> rankingCarrera = convocatoriaRepository.getRankingCarrera(id);

    return ConvocatoriaMapper.mapDetalleConvocatoria(convocatoria, auditEmpleadoResponse, cantidadPostulantes, tasas,
            rankingSocioeconomico, rankingCiclo, rankingCarrera);
  }

  // Tareas programadas: Actualizar el estado de las convocatorias a media noche

  @Transactional
  @Scheduled(cron = "0 0 0 * * *") // "0 0 0 * * *" para 1 hora, "0 0/15 * * * *" para 15 min
  @Retryable(
          retryFor = { TransactionSystemException.class },
          maxAttempts = 5,
          backoff = @Backoff(delay = 300000) // 5 minutos
  )
  public void actualizarEstadosConvocatorias() {
    System.out.println("Iniciando cron de actualización de convocatorias...");

    int aperturadas = convocatoriaRepository.aperturarConvocatoriasVigentes(LocalDate.now());
    int cerradas = convocatoriaRepository.cerrarConvocatoriasExpiradas(LocalDate.now());

    System.out.println("Cron exitoso: "+aperturadas+" abiertas, "+cerradas+" cerradas");

    // insertar la funcion para el procesamiento en lote
    if (cerradas>0) {
      int postulacionesActualizadas = evaluacionPostulanteService.evaluarPostulantes();
      System.out.println("Actualización exitosa de "+postulacionesActualizadas+" postulantes");
    }
  }

  @Recover
  public void recover(TransactionSystemException e) {
    System.out.println("ERROR CRÍTICO: El cron falló tras 5 intentos debido a un error de transacción. " +
            "Se requiere intervención manual");
  }
}
