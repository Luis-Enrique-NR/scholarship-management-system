package pe.com.security.scholarship.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pe.com.security.scholarship.dto.projection.RankingProjection;
import pe.com.security.scholarship.dto.projection.TasasConvocatoriaProjection;
import pe.com.security.scholarship.domain.entity.Convocatoria;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConvocatoriaRepository extends JpaRepository<Convocatoria, Integer> {

  @Override
  Optional<Convocatoria> findById(Integer id);

  @Query("SELECT c FROM Convocatoria c WHERE c.estado = 'APERTURADO'")
  Optional<Convocatoria> findConvocatoriaAperturada();

  @Query("SELECT c FROM Convocatoria c WHERE YEAR(c.fechaInicio) = :year")
  List<Convocatoria> findByYear(@Param("year") Integer year);

  @Query("SELECT COUNT(p) FROM Postulacion p WHERE p.convocatoria.id = :id")
  int getCantidadPostulantes(@Param("id") Integer id);

  @Query(value = "SELECT " +
          "  COUNT(p.id) / NULLIF(CAST(c.cantidad_vacantes AS numeric), 0) as tasaAceptacion, " +
          "  CASE WHEN COUNT(p.id) = 0 THEN 0 ELSE COUNT(m.id) / CAST(COUNT(p.id) AS NUMERIC) END as tasaMatriculados " +
          "FROM convocatorias c " +
          "LEFT JOIN postulaciones p ON c.id = p.id_convocatoria AND p.aceptado = true " +
          "LEFT JOIN matriculas m ON m.id_postulacion = p.id AND m.estado = 'ACEPTADO' " +
          "WHERE c.id = :id GROUP BY c.id, c.cantidad_vacantes", nativeQuery = true)
  TasasConvocatoriaProjection getTasasGenerales(Integer id);

  @Query(value = "SELECT COALESCE(es.nivel_socioeconomico, 'NINGUNO') AS etiqueta, COUNT(p.id) AS cantidad " +
          "FROM convocatorias c " +
          "LEFT JOIN postulaciones p ON c.id = p.id_convocatoria " +
          "LEFT JOIN estudiantes e ON e.id = p.id_estudiante " +
          "LEFT JOIN evaluaciones_socioeconomicas es ON es.id_estudiante = e.id " +
          "WHERE c.id = :id " +
          "GROUP BY c.id, etiqueta ORDER BY cantidad DESC", nativeQuery = true)
  List<RankingProjection> getRankingSocioeconomico(Integer id);

  @Query(value = """
    SELECT 
        COALESCE(CAST(pp.ciclo_relativo AS varchar), 'NINGUNO') AS etiqueta, 
        COUNT(p.id) AS cantidad 
    FROM convocatorias c 
    LEFT JOIN postulaciones p ON c.id = p.id_convocatoria 
    LEFT JOIN estudiantes e ON e.id = p.id_estudiante 
    LEFT JOIN promedios_ponderados pp ON pp.id_estudiante = e.id 
        AND pp.id_periodo = (
            SELECT per.id 
            FROM periodos_academicos per 
            WHERE per.fecha_fin < c.fecha_inicio 
            ORDER BY per.fecha_fin DESC 
            LIMIT 1
        ) 
    WHERE c.id = :id 
    GROUP BY COALESCE(CAST(pp.ciclo_relativo AS varchar), 'NINGUNO') 
    ORDER BY cantidad DESC 
    LIMIT 3
    """, nativeQuery = true)
  List<RankingProjection> getRankingCiclo(@Param("id") Integer id);

  @Query(value = "SELECT COALESCE(cr.nombre, 'NINGUNO') AS etiqueta, COUNT(p.id) AS cantidad " +
          "FROM convocatorias c " +
          "LEFT JOIN postulaciones p on c.id = p.id_convocatoria "+
          "LEFT JOIN estudiantes e ON e.id = p.id_estudiante " +
          "LEFT JOIN carreras cr ON cr.id = e.id_carrera " +
          "WHERE c.id = :id " +
          "GROUP BY etiqueta ORDER BY cantidad DESC LIMIT 3", nativeQuery = true)
  List<RankingProjection> getRankingCarrera(Integer id);

  @Modifying
  @Transactional
  @Query("UPDATE Convocatoria c SET c.estado = 'CERRADO' " +
          "WHERE c.estado = 'APERTURADO' AND c.fechaFin < :hoy")
  int cerrarConvocatoriasExpiradas(@Param("hoy") LocalDate hoy);

  @Modifying
  @Transactional
  @Query("UPDATE Convocatoria c SET c.estado = 'APERTURADO' " +
          "WHERE c.estado = 'PROGRAMADO' AND c.fechaInicio <= :hoy")
  int aperturarConvocatoriasVigentes(@Param("hoy") LocalDate hoy);

  @Query(value = "select * " +
          "from convocatorias c " +
          "where extract(year from c.fecha_fin) = extract(year from current_date) " +
          "and c.estado = 'CERRADO' " +
          "order by c.fecha_fin desc " +
          "limit 1", nativeQuery = true)
  Optional<Convocatoria> getUltimaConvocatoriaCerrada();
}
