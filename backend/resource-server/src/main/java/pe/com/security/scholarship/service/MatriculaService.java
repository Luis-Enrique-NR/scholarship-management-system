package pe.com.security.scholarship.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.security.scholarship.domain.entity.Curso;
import pe.com.security.scholarship.domain.entity.Estudiante;
import pe.com.security.scholarship.domain.entity.Matricula;
import pe.com.security.scholarship.domain.entity.Postulacion;
import pe.com.security.scholarship.domain.entity.Seccion;
import pe.com.security.scholarship.dto.projection.SeccionIntencionProjection;
import pe.com.security.scholarship.dto.request.SubmitMatriculaRequest;
import pe.com.security.scholarship.dto.response.CursoIntencionMatriculaResponse;
import pe.com.security.scholarship.dto.response.IntencionMatriculaResponse;
import pe.com.security.scholarship.dto.response.RegisteredMatriculaResponse;
import pe.com.security.scholarship.dto.response.SeccionIntencionMatriculaResponse;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.mapper.MatriculaMapper;
import pe.com.security.scholarship.repository.CursoRepository;
import pe.com.security.scholarship.repository.EstudianteRepository;
import pe.com.security.scholarship.repository.MatriculaRepository;
import pe.com.security.scholarship.repository.PostulacionRepository;
import pe.com.security.scholarship.repository.SeccionRepository;
import pe.com.security.scholarship.util.SecurityUtils;

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
  private final PostulacionService postulacionService;

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

    List<Curso> cursosPostulacion = cursoRepository.findByIdPostulacion(postulacion.getId());

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
}
