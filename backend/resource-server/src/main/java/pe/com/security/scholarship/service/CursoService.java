package pe.com.security.scholarship.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.security.scholarship.domain.entity.Curso;
import pe.com.security.scholarship.domain.entity.HorarioSeccion;
import pe.com.security.scholarship.domain.entity.Seccion;
import pe.com.security.scholarship.dto.request.RegisterCursoRequest;
import pe.com.security.scholarship.dto.response.RegisteredCursoResponse;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.mapper.CursoMapper;
import pe.com.security.scholarship.mapper.HorarioMapper;
import pe.com.security.scholarship.mapper.SeccionMapper;
import pe.com.security.scholarship.repository.CursoRepository;
import pe.com.security.scholarship.repository.HorarioSeccionRepository;
import pe.com.security.scholarship.repository.SeccionRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CursoService {
  private final CursoRepository cursoRepository;
  private final SeccionRepository seccionRepository;
  private final HorarioSeccionRepository horarioRepository;
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
}
