package pe.com.security.scholarship.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.security.scholarship.domain.entity.EvaluacionSocioeconomica;

import java.util.Optional;

@Repository
public interface EvaluacionSocioeconomicaRepository extends JpaRepository<EvaluacionSocioeconomica, Integer> {

  @Query(value = "SELECT es.* " +
          "FROM evaluaciones_socioeconomicas es " +
          "LEFT JOIN estudiantes e ON es.id_estudiante = e.id " +
          "WHERE e.codigo_estudiante = :codigoEstudiante " +
          "AND es.fecha_expiracion > CURRENT_DATE " +
          "ORDER BY es.fecha_evaluacion DESC LIMIT 1",
          nativeQuery = true)
  Optional<EvaluacionSocioeconomica> findEvaluacionActiva(@Param("codigoEstudiante") String codigoEstudiante);
}
