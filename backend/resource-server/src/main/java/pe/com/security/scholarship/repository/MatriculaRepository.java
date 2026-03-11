package pe.com.security.scholarship.repository;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.security.scholarship.domain.entity.Matricula;
import pe.com.security.scholarship.dto.projection.BecadoIntencionProjection;
import pe.com.security.scholarship.dto.projection.SeccionIntencionProjection;

import java.time.LocalDate;
import java.util.List;
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
          "count(m.id) as totalIntencionesPendientes " +
          "FROM secciones s " +
          "INNER JOIN cursos c ON s.id_curso = c.id " +
          "INNER JOIN matriculas m ON m.id_seccion = s.id and m.estado = 'PENDIENTE' " +
          "WHERE s.fecha_inicio > CURRENT_DATE " +
          "GROUP BY s.id, c.id " +
          "ORDER BY s.fecha_inicio ASC", nativeQuery = true)
  List<SeccionIntencionProjection> findIntencionesMatriculaSeccion();

  @Query(value = """
          select
          	m.id as idMatricula,
          	CONCAT(u.nombres, ' ', u.apellidos) as nombreCompleto,
          	e.codigo_estudiante as codigo,
          	p.promedio_general as promedioGeneral,
          	m.estado as estadoMatricula
          from matriculas m
          inner join postulaciones p
          	on p.id = m.id_postulacion
          inner join estudiantes e
          	on e.id = p.id_estudiante
          inner join usuarios u
          	on u.id = e.id_usuario
          where m.id_seccion = :idSeccion
  """, nativeQuery = true)
  List<BecadoIntencionProjection> findBecadosIntencionMatricula(@Param("idSeccion") Integer idSeccion);

  @Modifying
  @Query(value = """
          UPDATE matriculas m
          SET estado = 'ACEPTADO'
          FROM (
              SELECT id
              FROM matriculas
              WHERE id_seccion = :idSeccion
                AND estado = 'PENDIENTE'
              ORDER BY fecha_solicitud
              LIMIT :nuevasVacantes
          ) AS sub
          WHERE m.id = sub.id
  """, nativeQuery = true)
  int matricularPostulantes(@Param("idSeccion") Integer idSeccion, @Param("nuevasVacantes") Integer nuevasVacantes);

  @Modifying
  @Query(value = """
          UPDATE matriculas m
          SET estado = 'RECHAZADO'
          FROM secciones s
          WHERE m.id_seccion = s.id
            AND m.estado = 'PENDIENTE'
            AND s.fecha_inicio = :currentDate
  """, nativeQuery = true)
  int rechazarPostulantes(@Param("currentDate") LocalDate hoy);

  @Modifying
  @Query(value = """
          UPDATE matriculas m
          SET nota = :nota
          FROM postulaciones p
          INNER JOIN estudiantes e ON e.id = p.id_estudiante
          WHERE m.id_postulacion = p.id
            AND m.id_seccion = :idSeccion
            AND e.codigo_estudiante = :codigo
            AND m.estado = 'ACEPTADO'
  """, nativeQuery = true)
  int actualizarNota(@Param("codigo") String codigoEstudiante, @Param("idSeccion") Integer idSeccion,
                      @Param("nota") Double nota);
}
