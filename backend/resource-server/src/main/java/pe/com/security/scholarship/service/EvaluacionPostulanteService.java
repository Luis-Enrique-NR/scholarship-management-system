package pe.com.security.scholarship.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.security.scholarship.domain.entity.Convocatoria;
import pe.com.security.scholarship.dto.projection.PostulanteEvaluacionProjection;
import pe.com.security.scholarship.repository.ConvocatoriaRepository;
import pe.com.security.scholarship.repository.PostulacionRepository;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class EvaluacionPostulanteService {

  private final ConvocatoriaRepository convocatoriaRepository;
  private final PostulacionRepository postulacionRepository;

  @Transactional
  public int evaluarPostulantes() {
    Optional<Convocatoria> convocatoriaOpt = convocatoriaRepository.getUltimaConvocatoriaCerrada();

    if (convocatoriaOpt.isEmpty()) {
      System.out.println("Aviso: No se encontró convocatoria cerrada para evaluar en este año");
      return 0;
    }

    Convocatoria convocatoria = convocatoriaOpt.get();
    List<Integer> idsPostulaciones = postulacionRepository.findIdsByConvocatoria(convocatoria.getId());

    if (idsPostulaciones.isEmpty()) {
      System.out.println("Aviso: La convocatoria " + convocatoria.getId() + " no tiene postulaciones.");
      return 0;
    }

    List<PostulanteEvaluacionProjection> evaluaciones = postulacionRepository.getDatosEvaluacion(idsPostulaciones);

    // DRY: Un solo switch que define la lógica de cálculo (Lambda)
    switch (convocatoria.getModoEvaluacion()) {
      case PROMEDIO_PONDERADO ->
              procesarEvaluacion(evaluaciones, PostulanteEvaluacionProjection::getPromedioPonderado, "Promedio Ponderado");
      case SOCIOECONOMICO ->
              procesarEvaluacion(evaluaciones, p -> p.getValorEvalSocio() != null ? p.getValorEvalSocio().doubleValue() : null, "Evaluación Socioeconómica");
      case MIXTO ->
              procesarEvaluacion(evaluaciones, p -> (p.getPromedioPonderado() != null && p.getValorEvalSocio() != null)
                      ? (p.getPromedioPonderado() + p.getValorEvalSocio()) / 2.0 : null, "Mixto (Promedio/Socio)");
    }

    return postulacionRepository.actualizarPromedioGeneralPostulaciones(convocatoria.getId());
  }

  private void procesarEvaluacion(List<PostulanteEvaluacionProjection> lista,
                                  Function<PostulanteEvaluacionProjection, Double> extractor, // Recibe un objeto (Projection) y devuelve un número (Double).
                                  String nombreModo) {
    for (PostulanteEvaluacionProjection pEval : lista) {
      try {
        /* 1. EJECUCIÓN DE LA FÓRMULA (Lambda):
               Aquí no sabemos QUÉ estamos calculando (si es solo promedio, socio o mixto)
               Simplemente le decimos al 'extractor': "Aplica tu lógica a este postulante"
               La lógica real fue definida arriba en el 'switch' como una función flecha (->)
        */
        Double valorCalculado = extractor.apply(pEval);

        /* 2. VALIDACIÓN DE NULOS (Tu regla de auditoría):
               Si la fórmula devolvió null (porque faltaba un dato),
               usamos 'continue' para saltar al siguiente postulante del bucle 'for'
               sin llegar a la línea del 'update'
        */
        if (valorCalculado == null) {
          System.out.println("Postulante ID: " + pEval.getIdPostulacion() + " - omitido: Datos nulos para modo " + nombreModo);
          continue;
        }

        /* 3. PERSISTENCIA DIRIGIDA:
               Llamamos al Query nativo @Modifying que creamos
        */
        postulacionRepository.actualizarPromedioGeneral(pEval.getIdPostulacion(), valorCalculado);
      } catch (Exception e) {
        System.out.println("Error crítico en postulante " + pEval.getIdPostulacion() + ": " + e.getMessage());
      }
    }
  }
}
