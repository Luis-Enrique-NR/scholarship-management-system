package pe.com.security.scholarship.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.security.scholarship.domain.entity.Convocatoria;
import pe.com.security.scholarship.domain.entity.Curso;
import pe.com.security.scholarship.domain.entity.Estudiante;
import pe.com.security.scholarship.domain.entity.Postulacion;
import pe.com.security.scholarship.dto.request.RegisterPostulacionRequest;
import pe.com.security.scholarship.dto.response.CursoPostulacionResponse;
import pe.com.security.scholarship.dto.response.RegisteredPostulacionResponse;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.mapper.CursoMapper;
import pe.com.security.scholarship.mapper.PostulacionMapper;
import pe.com.security.scholarship.repository.ConvocatoriaRepository;
import pe.com.security.scholarship.repository.CursoRepository;
import pe.com.security.scholarship.repository.EstudianteRepository;
import pe.com.security.scholarship.repository.MatriculaRepository;
import pe.com.security.scholarship.repository.PostulacionRepository;
import pe.com.security.scholarship.util.SecurityUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostulacionService {

  private final PostulacionRepository postulacionRepository;
  private final ConvocatoriaRepository convocatoriaRepository;
  private final EstudianteRepository estudianteRepository;
  private final CursoRepository cursoRepository;
  private final MatriculaRepository matriculaRepository;

  @Transactional
  public RegisteredPostulacionResponse registerPostulacion(RegisterPostulacionRequest request) {
    Convocatoria convocatoria = convocatoriaRepository.findById(request.getIdConvocatoria())
            .orElseThrow(() -> new NotFoundException("No se encontró la convocatoria"));

    UUID idUsuario = SecurityUtils.getCurrentUserId();
    Estudiante estudiante = estudianteRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NotFoundException("No se encontró estudiante con el user id del payload"));

    // No puede postular más de una vez a la misma convocatoria
    if (postulacionRepository.existsByEstudianteIdAndConvocatoriaId(estudiante.getId(), convocatoria.getId())) {
      throw new BadRequestException("Solo puedes postular una vez a la convocatoria");
    }

    int numeroBecas = postulacionRepository.cantidadBecasByYear(estudiante.getId());

    // Ha sido becado este año?
    if (numeroBecas>0) {
      if (tieneBecaActiva(estudiante.getId())) throw new BadRequestException("Aún tienes una beca vigente");
      // Solo puede recibir 3 becas por año
      if (numeroBecas==3) throw new BadRequestException("Ya alcanzaste el límite de 3 becas por año");
      // Debe haber aprobado con 15 en su última matrícula (si aplica)
      if (matriculaRepository.notaUltimaMatricula(estudiante.getId())<15) throw new BadRequestException("Tu última nota debe ser mínima 15");
    }

    List<Curso> cursoList = cursoRepository.findAllById(request.getIdsCursos());

    if (cursoList.size() != request.getIdsCursos().size()) throw new NotFoundException("Uno o más cursos no fueron encontrados");

    Postulacion postulacion = PostulacionMapper.buildPostulacion(convocatoria, estudiante, new HashSet<>(cursoList));
    postulacionRepository.save(postulacion);

    Set<CursoPostulacionResponse> cursoSet = cursoList.stream().map(CursoMapper::mapCursoPostulacion)
            .collect(Collectors.toSet());

    return PostulacionMapper.mapRegisteredPostulacion(postulacion, cursoSet);
  }

  // VERIFICAR SI TIENE BECA ACTIVA
  public boolean tieneBecaActiva(UUID idEstudiante) {

    // Obtener su ultima beca
    Postulacion lastPostulacion = postulacionRepository.findLastPostulacion(idEstudiante)
            .orElseThrow(() -> new NotFoundException("No se encontró beca para el presente año"));

    // Se matriculó?
    if (matriculaRepository.seMatriculo(lastPostulacion.getId())) return false;

    // Menos de 3 meses?
    return postulacionRepository.cantidadMesesBeca(idEstudiante)<=3;
  }
}
