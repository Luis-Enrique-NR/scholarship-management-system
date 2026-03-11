package pe.com.security.scholarship.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.security.scholarship.domain.entity.Curso;
import pe.com.security.scholarship.domain.entity.Empleado;
import pe.com.security.scholarship.domain.entity.HorarioSeccion;
import pe.com.security.scholarship.domain.entity.Seccion;
import pe.com.security.scholarship.domain.enums.DiaSemana;
import pe.com.security.scholarship.dto.request.RegisterHorarioSeccionRequest;
import pe.com.security.scholarship.dto.request.RegisterSeccionRequest;
import pe.com.security.scholarship.dto.request.UpdateVacantesSeccionRequest;
import pe.com.security.scholarship.dto.response.RegisteredSeccionResponse;
import pe.com.security.scholarship.dto.response.UpdatedVacantesSeccionResponse;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.mapper.HorarioMapper;
import pe.com.security.scholarship.mapper.SeccionMapper;
import pe.com.security.scholarship.repository.CursoRepository;
import pe.com.security.scholarship.repository.EmpleadoRepository;
import pe.com.security.scholarship.repository.MatriculaRepository;
import pe.com.security.scholarship.repository.SeccionRepository;
import pe.com.security.scholarship.util.SecurityUtils;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeccionService {

  private final SeccionRepository seccionRepository;
  private final CursoRepository cursoRepository;
  private final MatriculaRepository matriculaRepository;
  private final EmpleadoRepository empleadoRepository;

  @Transactional
  public RegisteredSeccionResponse register(RegisterSeccionRequest request, Integer idCurso) {
    Curso curso = cursoRepository.findById(idCurso)
            .orElseThrow(() -> new NotFoundException("No se encontró curso con el ID ingresado"));

    Seccion nuevaSeccion = SeccionMapper.buildSeccion(request.getFechaInicio(), curso, request.getVacantesDisponibles());

    // Verificar que los horarios sean correctos
    horariosValidos(request.getHorarios());

    List<HorarioSeccion> horarios = request.getHorarios().stream()
            .map(h -> HorarioMapper.buildHorario(h.getDiaSemana(), h.getHoraInicio(),
                    h.getHoraFin(), nuevaSeccion)).toList();

    nuevaSeccion.setHorarios(horarios);

    seccionRepository.save(nuevaSeccion);

    return SeccionMapper.mapRegisteredSeccion(nuevaSeccion);
  }

  public void horariosValidos(List<RegisterHorarioSeccionRequest> horariosRequest) {

    for (RegisterHorarioSeccionRequest h : horariosRequest) {
      if (!h.getHoraFin().isAfter(h.getHoraInicio())) {
        throw new BadRequestException(String.format(
                "Horario inconsistente en el día (%s): La hora de fin (%s) debe ser posterior a la de inicio (%s)",
                h.getDiaSemana(), h.getHoraFin(), h.getHoraInicio()));
      }
    }

    Map<DiaSemana, List<RegisterHorarioSeccionRequest>> horariosDia = horariosRequest.stream()
            .collect(Collectors.groupingBy(RegisterHorarioSeccionRequest::getDiaSemana));

    horariosDia.forEach((diaSemana, lista) -> {
      if (lista.size()>1) hayCruceHorario(diaSemana, lista);
    });
  }

  public void hayCruceHorario(DiaSemana dia, List<RegisterHorarioSeccionRequest> lista) {
    List<RegisterHorarioSeccionRequest> listaOrdenada = lista.stream()
            .sorted(Comparator.comparing(RegisterHorarioSeccionRequest::getHoraInicio))
            .toList();

    for (int i=0; i<listaOrdenada.size()-1; i++) {
      LocalTime finActual = listaOrdenada.get(i).getHoraFin();
      LocalTime inicioProximo = listaOrdenada.get(i+1).getHoraInicio();

      if (finActual.isAfter(inicioProximo))  {
        throw new BadRequestException(String.format(
                "Cruce el %s: El bloque %s-%s se traslapa con %s-%s",
                dia, listaOrdenada.get(i).getHoraInicio(), finActual,
                inicioProximo, listaOrdenada.get(i + 1).getHoraFin()));
      }
    }
  }

  @Transactional
  public UpdatedVacantesSeccionResponse updateVacantes(UpdateVacantesSeccionRequest request) {
    UUID idUsuario = SecurityUtils.getCurrentUserId();
    Empleado empleado = empleadoRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NotFoundException("Empleado no encontrado"));

    Seccion seccion = seccionRepository.findById(request.getIdSeccion())
            .orElseThrow(() -> new NotFoundException("No se encontró la sección con el ID ingresado"));

    if (seccion.getVacantesDisponibles()!=null && seccion.getVacantesDisponibles() >= request.getCantidadVacantes()) {
      throw new BadRequestException("La nueva cantidad de vacantes debe superar a las disponibles actualmente");
    }

    int nuevasVacantes = seccion.getVacantesDisponibles() == null ?
            request.getCantidadVacantes() :
            request.getCantidadVacantes() - seccion.getVacantesDisponibles();

    seccion.setVacantesDisponibles(request.getCantidadVacantes());
    seccionRepository.save(seccion);

    int nuevosMatriculados = matriculaRepository.matricularPostulantes(request.getIdSeccion(), nuevasVacantes, empleado.getId());

    return SeccionMapper.mapUpdatedVacantes(nuevosMatriculados, seccion.getVacantesDisponibles());
  }
}
