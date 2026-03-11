package pe.com.security.scholarship.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Limit;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.com.security.scholarship.domain.entity.Curso;
import pe.com.security.scholarship.domain.entity.Empleado;
import pe.com.security.scholarship.domain.entity.Estudiante;
import pe.com.security.scholarship.domain.entity.Matricula;
import pe.com.security.scholarship.domain.entity.Postulacion;
import pe.com.security.scholarship.domain.entity.Seccion;
import pe.com.security.scholarship.domain.enums.EstadoMatricula;
import pe.com.security.scholarship.dto.ProcesamientoResult;
import pe.com.security.scholarship.dto.projection.SeccionIntencionProjection;
import pe.com.security.scholarship.dto.request.AprobarMatriculaRequest;
import pe.com.security.scholarship.dto.request.NotaCsvRequest;
import pe.com.security.scholarship.dto.request.SubmitMatriculaRequest;
import pe.com.security.scholarship.dto.response.BecadoIntencionMatriculaResponse;
import pe.com.security.scholarship.dto.response.CursoIntencionMatriculaResponse;
import pe.com.security.scholarship.dto.response.IntencionMatriculaResponse;
import pe.com.security.scholarship.dto.response.RegisteredMatriculaResponse;
import pe.com.security.scholarship.dto.response.SeccionBecadosResponse;
import pe.com.security.scholarship.dto.response.SeccionIntencionMatriculaResponse;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.mapper.MatriculaMapper;
import pe.com.security.scholarship.repository.CursoRepository;
import pe.com.security.scholarship.repository.EmpleadoRepository;
import pe.com.security.scholarship.repository.EstudianteRepository;
import pe.com.security.scholarship.repository.MatriculaRepository;
import pe.com.security.scholarship.repository.PostulacionRepository;
import pe.com.security.scholarship.repository.SeccionRepository;
import pe.com.security.scholarship.util.CargaMasivaHelper;
import pe.com.security.scholarship.util.SecurityUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatriculaService {

  private final EstudianteRepository estudianteRepository;
  private final PostulacionRepository postulacionRepository;
  private final CursoRepository cursoRepository;
  private final SeccionRepository seccionRepository;
  private final MatriculaRepository matriculaRepository;
  private final EmpleadoRepository empleadoRepository;
  private final PostulacionService postulacionService;
  private final CargaMasivaHelper cargaMasivaHelper;

  @Transactional
  public RegisteredMatriculaResponse submitEnrollmentIntention(SubmitMatriculaRequest request) {
    UUID idUsuario = SecurityUtils.getCurrentUserId();
    Estudiante estudiante = estudianteRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NotFoundException("No se encontró estudiante asociado al id del payload"));

    if (!postulacionService.tieneBecaActiva(estudiante.getId())) throw new BadRequestException("No tienes una beca vigente");

    // Ya hay una intencion de matricula pendiente
    if(matriculaRepository.existsIntencionMatricula(estudiante.getId())) throw new BadRequestException("Ya tienes una intención de matrícula pendiente");

    Postulacion postulacion = postulacionRepository.findLastPostulacion(estudiante.getId())
            .orElseThrow(() -> new NotFoundException("No se encontró postulación para el presente año"));

    List<Curso> cursosPostulacion = cursoRepository.findByIdPostulacion(postulacion.getId(), LocalDate.now());

    Seccion seccionMatricula = seccionRepository.findById(request.getIdSeccion())
            .orElseThrow(() -> new NotFoundException("No se encontró sección con el ID ingresado"));

    if (!cursosPostulacion.contains(seccionMatricula.getCurso()) || !seccionMatricula.getFechaInicio().isAfter(LocalDate.now())) {
      throw new BadRequestException("Sección no válida para la intención de matrícula");
    }

    Matricula matricula = matriculaRepository.save(MatriculaMapper.buildMatricula(seccionMatricula, postulacion));

    return MatriculaMapper.mapSubmitMatricula(matricula);
  }

  @Transactional(readOnly = true)
  public IntencionMatriculaResponse getIntencionMatricula() {
    UUID idUsuario = SecurityUtils.getCurrentUserId();
    Estudiante estudiante = estudianteRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NotFoundException("No se encontró estudiante asociado al id del payload"));

    Matricula matricula = matriculaRepository.findLastMatriculaByIdEstudiante(estudiante.getId(), Limit.of(1))
            .stream()
            .findFirst().orElseThrow(() -> new NotFoundException("No se encontró matrícula asociada al estudiante"));

    return MatriculaMapper.mapIntencionMatricula(matricula);
  }

  public List<CursoIntencionMatriculaResponse> getIntencionesMatriculaSeccion() {

    // Se obtiene la lista de proyecciones con datos puntuales de la BD, esto ya viene "ordenado" de la BD
    List<SeccionIntencionProjection> proyecciones = matriculaRepository.findIntencionesMatriculaSeccion();

    if (proyecciones == null || proyecciones.isEmpty()) {
      return Collections.emptyList();
    }

    class TemporalAgrupador {
      final Integer idCurso;
      final CursoIntencionMatriculaResponse curso;
      final SeccionIntencionMatriculaResponse seccion;

      TemporalAgrupador(SeccionIntencionProjection p) {
        this.idCurso = p.getIdCurso();
        this.curso = MatriculaMapper.mapCursoIntencionMatricula(p);
        this.seccion = MatriculaMapper.mapSeccionIntencionMatricula(p);
      }
    }

    List<TemporalAgrupador> temporales = proyecciones.stream()
            .map(TemporalAgrupador::new)
            .toList();

    // Se crea un map agrupando secciones por cursos
    Map<Integer, List<TemporalAgrupador>> agrupado = temporales.stream()
            .collect(Collectors.groupingBy(t -> t.idCurso));

    return agrupado.values().stream()
            .map(listaTemporal -> {
              // Tomamos la primera proyección para datos generales del curso
              TemporalAgrupador primero = listaTemporal.getFirst();
              CursoIntencionMatriculaResponse cursoFinal = primero.curso;

              // Seteamos las secciones mapeadas reales al objeto curso
              cursoFinal.setSecciones(listaTemporal.stream()
                      .map(t -> t.seccion)
                      .toList());

              return cursoFinal;
            }).sorted(Comparator.comparing(
                    // Ordenamos usando datos reales de los DTOs ya agrupados
                    c -> c.getSecciones().getFirst().getFechaInicio()
            )).toList();
  }

  @Transactional(readOnly = true)
  public SeccionBecadosResponse getBecadosSeccion(Integer idSeccion) {
    Seccion seccion = seccionRepository.findById(idSeccion)
            .orElseThrow(() -> new NotFoundException("No se encontró la sección con el ID enviado"));

    int vacantesDisponibles = seccionRepository.getVacantesRestantes(idSeccion);

    List<BecadoIntencionMatriculaResponse> becados = matriculaRepository.findBecadosIntencionMatricula(idSeccion).stream()
            .map(MatriculaMapper::mapBecadoIntencionMatricula)
            .toList();

    return MatriculaMapper.mapSeccionBecados(seccion, vacantesDisponibles, becados);
  }

  @Transactional
  public void actualizarEstadoMatricula(AprobarMatriculaRequest request) {
    UUID idUsuario = SecurityUtils.getCurrentUserId();
    Empleado empleado = empleadoRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NotFoundException("No se encontró empleado con el ID del payload"));

    Matricula matricula = matriculaRepository.findById(request.getIdMatricula())
            .orElseThrow(() -> new NotFoundException("No se encontró la matrícula con el ID enviado"));

    // Evitar repetir el mismo estado
    if (matricula.getEstado() == (request.getAprobado() ? EstadoMatricula.ACEPTADO : EstadoMatricula.RECHAZADO)) {
      throw new BadRequestException("No se puede repetir el mismo estado");
    }

    // No se puede rechazar una matrícula ya aprobada
    if (matricula.getEstado() == EstadoMatricula.ACEPTADO) {
      throw new BadRequestException("No se puede rechazar una matrícula ya aprobada");
    }

    matricula.setEmpleado(empleado);

    matricula.setFechaMatricula(request.getAprobado() ? Instant.now() : null);

    matricula.setEstado(request.getAprobado() ? EstadoMatricula.ACEPTADO : EstadoMatricula.RECHAZADO);
  }

  @Transactional
  public ProcesamientoResult procesarCargaNotas(MultipartFile file, Integer idSeccion) {
    Seccion seccion = seccionRepository.findById(idSeccion)
            .orElseThrow(() -> new NotFoundException("Sección no encontrada"));

    if (LocalDate.now().isBefore(seccion.getFechaInicio())) {
      throw new BadRequestException("No se pueden subir notas antes del inicio de la sección");
    }

    return cargaMasivaHelper.procesar(file, NotaCsvRequest.class, fila -> {
      if (fila.getNota() < 0 || fila.getNota() > 20) {
        throw new BadRequestException("La nota debe estar entre 0 y 20");
      }

      int filasAfectadas = matriculaRepository.actualizarNota(fila.getCodigo(), idSeccion, fila.getNota());

      if (filasAfectadas == 0) {
        throw new BadRequestException("El estudiante " + fila.getCodigo() + " no pertenece a esta sección");
      }
    });
  }

  @Scheduled(cron = "0 5 0 * * *")
  public void ejecutarCron() {
    System.out.println("Iniciando cron de actualización...");
    try {
      int total = procesarRechazos();
      System.out.println("Cron exitoso: "+total+" intenciones de matrícula rechazadas");
    } catch (Exception e) {
      System.out.println("El cron falló definitivamente tras los reintentos");
    }
  }

  @Transactional
  @Retryable(
          retryFor = { TransactionSystemException.class, DataAccessException.class },
          maxAttempts = 5,
          backoff = @Backoff(delay = 300000)
  )
  public int procesarRechazos() {
    return matriculaRepository.rechazarPostulantes(LocalDate.now());
  }

  @Recover
  public void recover(TransactionSystemException e) {
    System.out.println("ERROR CRÍTICO: El cron falló tras 5 intentos debido a un error de transacción. " +
            "Se requiere intervención manual");
  }
}
