package pe.com.security.scholarship.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.security.scholarship.domain.entity.Seccion;
import pe.com.security.scholarship.dto.request.UpdateVacantesSeccionRequest;
import pe.com.security.scholarship.dto.response.UpdatedVacantesSeccionResponse;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.mapper.SeccionMapper;
import pe.com.security.scholarship.repository.MatriculaRepository;
import pe.com.security.scholarship.repository.SeccionRepository;

@Service
@RequiredArgsConstructor
public class SeccionService {

  private final SeccionRepository seccionRepository;
  private final MatriculaRepository matriculaRepository;

  @Transactional
  public UpdatedVacantesSeccionResponse updateVacantes(UpdateVacantesSeccionRequest request) {
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

    int nuevosMatriculados = matriculaRepository.matricularPostulantes(request.getIdSeccion(), nuevasVacantes);

    return SeccionMapper.mapUpdatedVacantes(nuevosMatriculados, seccion.getVacantesDisponibles());
  }
}
