package pe.com.security.scholarship.repository;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.security.scholarship.domain.entity.Matricula;
import pe.com.security.scholarship.dto.projection.SeccionIntencionProjection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatriculaRepository extends JpaRepository<Matricula, Integer> {

  @Query(value = """
          select COALESCE(m.nota, 0.0)
          from postulaciones p
          left join matriculas m
          	on p.id = m.id_postulacion
          where p.id_estudiante = :idEstudiante
          and p.aceptado is true
          and EXTRACT(year from p.fecha_postulacion) = EXTRACT(year from CURRENT_DATE)
          order by p.fecha_postulacion desc
          limit 1
    """, nativeQuery = true)
  Double notaUltimaMatricula(@Param("idEstudiante") UUID idEstudiante);

  @Query(value = """
          SELECT EXISTS (
              SELECT 1
              FROM matriculas
              WHERE id_postulacion = :idPostulacion
              AND estado = 'ACEPTADO'
          )
  """, nativeQuery = true)
  boolean seMatriculo(@Param("idPostulacion") Integer idPostulacion);

  @Query(value = """
          SELECT EXISTS (
              SELECT 1
              FROM matriculas m
              INNER JOIN postulaciones p
                  ON p.id = m.id_postulacion
              WHERE p.id_estudiante = :idEstudiante
              AND estado = 'PENDIENTE'
          )
  """, nativeQuery = true)
  boolean existsIntencionMatricula(@Param("idEstudiante") UUID idEstudiante);

  @Query("SELECT m FROM Matricula m " +
          "JOIN FETCH m.seccion s " +
          "JOIN FETCH s.curso c " +
          "JOIN m.postulacion p " +
          "WHERE p.estudiante.id = :idEstudiante " +
          "ORDER BY m.fechaSolicitud DESC")
  List<Matricula> findLastMatriculaByIdEstudiante(@Param("idEstudiante") UUID idEstudiante, Limit limit);

  @Query(value = "SELECT s.id as idSeccion, s.fecha_inicio as fechaInicio, " +
          "c.id as idCurso, c.nombre as nombreCurso, c.codigo as codigoCurso, " +
          "count(m.id) as totalIntencionesMatricula " +
          "FROM secciones s " +
          "INNER JOIN cursos c ON s.id_curso = c.id " +
          "INNER JOIN matriculas m ON m.id_seccion = s.id " +
          "WHERE s.fecha_inicio > CURRENT_DATE " +
          "GROUP BY s.id, c.id " +
          "ORDER BY s.fecha_inicio ASC", nativeQuery = true)
  List<SeccionIntencionProjection> findIntencionesMatriculaSeccion();
}
