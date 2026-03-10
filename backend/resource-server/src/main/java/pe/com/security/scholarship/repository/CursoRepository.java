package pe.com.security.scholarship.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.security.scholarship.domain.entity.Curso;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CursoRepository extends JpaRepository<Curso, Integer> {

  @Query("""
           SELECT DISTINCT c
           FROM Curso c
           JOIN FETCH c.secciones s
           JOIN c.postulaciones p
           WHERE p.id = :idPostulacion
           AND s.fechaInicio > :fechaReferencia
  """)
  List<Curso> findByIdPostulacion(@Param("idPostulacion") Integer idPostulacion, @Param("fechaReferencia") LocalDate fecha);

  boolean existsByCodigo(String codigo);

  @Query("""
           SELECT c.id
           FROM Curso c JOIN c.secciones s
           WHERE s.fechaInicio > :fechaReferencia
           GROUP BY c.id, c.nombre, c.modalidad
           ORDER BY MIN(s.fechaInicio) ASC
  """)
  Page<Integer> findIdsCursosHorarios(@Param("fechaReferencia") LocalDate fecha, Pageable pageable);

  @Query("""
           SELECT DISTINCT c
           FROM Curso c
           LEFT JOIN FETCH c.secciones s
           WHERE c.id IN :ids
           AND s.fechaInicio > :fechaReferencia
  """)
  List<Curso> findCursosSecciones(@Param("ids") List<Integer> ids, @Param("fechaReferencia") LocalDate fecha, Sort sort);
}
