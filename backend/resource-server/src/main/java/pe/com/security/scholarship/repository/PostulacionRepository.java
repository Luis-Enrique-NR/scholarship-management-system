package pe.com.security.scholarship.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pe.com.security.scholarship.domain.entity.Postulacion;
import pe.com.security.scholarship.dto.projection.PostulanteEvaluacionProjection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostulacionRepository extends JpaRepository<Postulacion, Integer> {

  boolean existsByEstudianteIdAndConvocatoriaId(UUID estudianteId, Integer convocatoriaId);

  @Query(value = """
          select count(p.id)
          from postulaciones p
          where p.id_estudiante = :idEstudiante
          and p.aceptado is true
          and extract(year from p.fecha_postulacion) = extract(year from CURRENT_DATE)
    """, nativeQuery = true)
  int cantidadBecasByYear(@Param("idEstudiante") UUID idEstudiante);

  @Query(value = """
          select date_part('month', CURRENT_DATE) - date_part('month', p.fecha_postulacion)
          from postulaciones p
          where p.id_estudiante = :idEstudiante
          and p.aceptado is true
          and EXTRACT(year from p.fecha_postulacion) = EXTRACT(year from CURRENT_DATE)
          order by p.fecha_postulacion desc
          limit 1
    """, nativeQuery = true)
  int cantidadMesesBeca(@Param("idEstudiante") UUID idEstudiante);

  @Query(value = """
          select p.*
          from postulaciones p
          where p.id_estudiante = :idEstudiante
          and p.aceptado is true
          and EXTRACT(year from p.fecha_postulacion) = EXTRACT(year from CURRENT_DATE)
          order by p.fecha_postulacion desc
          limit 1
    """, nativeQuery = true)
  Optional<Postulacion> findLastPostulacion(@Param("idEstudiante") UUID idEstudiante);

  @Query("SELECT p FROM Postulacion p WHERE p.estudiante.id = :idEstudiante AND YEAR(p.fechaPostulacion) = :year")
  List<Postulacion> findByYear(@Param("idEstudiante") UUID idEstudiante, @Param("year") Integer year);

  @Query(value = """
          WITH PeriodoReferencia AS (
              -- Identificar el periodo académico correspondiente
              SELECT id
              FROM periodos_academicos
              WHERE fecha_fin < CURRENT_DATE
              ORDER BY fecha_fin DESC
              LIMIT 1
          ),
          UltimaEval AS (
              -- Obtener solo la evaluación vigente más reciente por estudiante
              SELECT DISTINCT ON (id_estudiante)
                  id_estudiante,
                  CASE nivel_socioeconomico
                      WHEN 'DEFICIENTE' THEN 20
                      WHEN 'REGULAR' THEN 15
                      WHEN 'BUENO' THEN 10
                  END AS valor
              FROM evaluaciones_socioeconomicas
              WHERE fecha_expiracion > CURRENT_DATE
              ORDER BY id_estudiante, created_at DESC
          ),
          UltimoPromedio AS (
              -- Obtener el promedio solo si existe en el periodo de referencia
              SELECT DISTINCT ON (pp.id_estudiante)
                  pp.id_estudiante,
                  pp.promedio_ponderado
              FROM promedios_ponderados pp
              JOIN PeriodoReferencia pr ON pp.id_periodo = pr.id
              ORDER BY pp.id_estudiante, pp.created_at DESC
          )
          SELECT
              p.id AS idPostulacion,
              ev.valor AS valorEvalSocio,
              up.promedio_ponderado AS promedioPonderado
          FROM postulaciones p
          LEFT JOIN UltimaEval ev ON p.id_estudiante = ev.id_estudiante
          LEFT JOIN UltimoPromedio up ON p.id_estudiante = up.id_estudiante
          WHERE p.id IN :listPostulaciones
    """, nativeQuery = true)
  List<PostulanteEvaluacionProjection> getDatosEvaluacion(@Param("listPostulaciones") List<Integer> listPostulaciones);

  @Query("SELECT p.id FROM Postulacion p WHERE p.convocatoria.id = :idConvocatoria")
  List<Integer> findIdsByConvocatoria(@Param("idConvocatoria") Integer idConvocatoria);

  @Modifying
  @Transactional
  @Query(value = """
        UPDATE postulaciones p
        SET aceptado = sub.es_ganador
        FROM (
            SELECT 
                p2.id,
                (p2.promedio_general IS NOT NULL AND 
                 ROW_NUMBER() OVER (
                     ORDER BY p2.promedio_general DESC NULLS LAST
                 ) <= c.cantidad_vacantes) as es_ganador
            FROM postulaciones p2
            INNER JOIN convocatorias c ON c.id = p2.id_convocatoria
            WHERE p2.id_convocatoria = :idConvocatoria
        ) AS sub
        WHERE p.id = sub.id 
          AND p.id_convocatoria = :idConvocatoria
    """, nativeQuery = true)
  int actualizarPromedioGeneralPostulaciones(@Param("idConvocatoria") Integer idConvocatoria);

  @Modifying
  @Query("UPDATE Postulacion p SET p.promedioGeneral = :promedio WHERE p.id = :id")
  void actualizarPromedioGeneral(@Param("id") Integer id, @Param("promedio") Double promedio);
}
