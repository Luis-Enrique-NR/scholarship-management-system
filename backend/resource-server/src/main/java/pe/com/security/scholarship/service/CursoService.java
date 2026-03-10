package pe.com.security.scholarship.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.security.scholarship.domain.entity.Curso;
import pe.com.security.scholarship.domain.entity.Estudiante;
import pe.com.security.scholarship.domain.entity.HorarioSeccion;
import pe.com.security.scholarship.domain.entity.Postulacion;
import pe.com.security.scholarship.domain.entity.Seccion;
import pe.com.security.scholarship.dto.request.RegisterCursoRequest;
import pe.com.security.scholarship.dto.response.OverviewCursoResponse;
import pe.com.security.scholarship.dto.response.OverviewSeccionResponse;
import pe.com.security.scholarship.dto.response.RegisteredCursoResponse;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.mapper.CursoMapper;
import pe.com.security.scholarship.mapper.HorarioMapper;
import pe.com.security.scholarship.mapper.SeccionMapper;
import pe.com.security.scholarship.repository.CursoRepository;
import pe.com.security.scholarship.repository.EstudianteRepository;
import pe.com.security.scholarship.repository.PostulacionRepository;
import pe.com.security.scholarship.util.SecurityUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CursoService {
  private final CursoRepository cursoRepository;
  private final EstudianteRepository estudianteRepository;
  private final PostulacionRepository postulacionRepository;
  private final PostulacionService postulacionService;
  private final SeccionService seccionService;

  @Transactional
  public RegisteredCursoResponse register(RegisterCursoRequest request) {
    if (cursoRepository.existsByCodigo(request.getCodigo())) {
      throw new BadRequestException("Ya existe un curso con el código ingresado");
    }

    if (request.getSecciones()!=null) {
      request.getSecciones().forEach(s -> seccionService.horariosValidos(s.getHorarios()));
    }

    Curso curso = CursoMapper
            .buildCurso(request.getNombre(), request.getCodigo(), request.getModalidadCurso());

    List<Seccion> secciones = Optional.ofNullable(request.getSecciones())
            .orElse(Collections.emptyList())
            .stream()
            .map(seccionRequest -> {
              Seccion seccion = SeccionMapper
                              .buildSeccion(seccionRequest.getFechaInicio(), curso, seccionRequest.getVacantesDisponibles());

              List<HorarioSeccion> horarios = seccionRequest.getHorarios()
                      .stream()
                      .map(h -> HorarioMapper
                              .buildHorario(h.getDiaSemana(), h.getHoraInicio(), h.getHoraFin(), seccion))
                      .toList();

              seccion.setHorarios(horarios);
              return seccion;
            }).toList();

    curso.setSecciones(secciones);

    Curso cursoRegistrado = cursoRepository.save(curso);

    return CursoMapper.mapRegisteredCurso(cursoRegistrado);
  }

  @Transactional(readOnly = true)
  public Page<OverviewCursoResponse> getCatalogo(Pageable pageable) {
    validarSort(pageable.getSort());

    return cursoRepository.findAll(pageable).map(CursoMapper::mapCatalogo);
  }

  @Transactional(readOnly = true)
  public Page<OverviewCursoResponse> getHorarios(Pageable pageable) {
    validarSort(pageable.getSort());
    LocalDate hoy = LocalDate.now();

    Page<Integer> idsPage = cursoRepository.findIdsCursosHorarios(hoy, pageable);
    if (idsPage.isEmpty()) return Page.empty(pageable);

    List<Curso> listaCursos = cursoRepository.findCursosSecciones(idsPage.getContent(), hoy, pageable.getSort());

    List<OverviewCursoResponse> contenido = listaCursos.stream()
            .map(curso -> {
              List<OverviewSeccionResponse> seccionesDTO = curso.getSecciones().stream()
                      .sorted(Comparator.comparing(Seccion::getFechaInicio))
                      .map(SeccionMapper::mapOverviewSeccion)
                      .toList();
              return CursoMapper.mapHorario(curso, seccionesDTO);
            })
            .toList();

    return new PageImpl<>(contenido, pageable, idsPage.getTotalElements());
  }

  @Transactional(readOnly = true)
  public List<OverviewCursoResponse> getOfertaDisponiblePorBeca() {
    UUID idUsuario = SecurityUtils.getCurrentUserId();
    Estudiante estudiante = estudianteRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NotFoundException("No se encontró estudiante asociado al id del payload"));

    if (!postulacionService.tieneBecaActiva(estudiante.getId())) throw new BadRequestException("No tienes una beca vigente");

    Postulacion postulacion = postulacionRepository.findLastPostulacion(estudiante.getId())
            .orElseThrow(() -> new NotFoundException("No se encontró postulación para el presente año"));

    List<Curso> cursosPostulacion = cursoRepository.findByIdPostulacion(postulacion.getId(), LocalDate.now());

    return cursosPostulacion.stream()
            .map(curso -> {
              List<OverviewSeccionResponse> seccionesDTO = curso.getSecciones().stream()
                      .sorted(Comparator.comparing(Seccion::getFechaInicio))
                      .map(SeccionMapper::mapOverviewSeccion)
                      .toList();
              return CursoMapper.mapHorario(curso, seccionesDTO);
            })
            .toList();
  }

  // Metodos complementarios

  private void validarSort(Sort sort) {
    sort.forEach(order -> {
      if (!ORDENAMIENTOS_PERMITIDOS.contains(order.getProperty())) {
        throw new BadRequestException("Campo de ordenamiento no permitido: " + order.getProperty());
      }
    });
  }

  private static final Set<String> ORDENAMIENTOS_PERMITIDOS = Set.of("nombre", "modalidad");
}
