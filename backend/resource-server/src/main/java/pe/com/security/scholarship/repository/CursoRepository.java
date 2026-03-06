package pe.com.security.scholarship.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.security.scholarship.domain.entity.Curso;

import java.util.List;

@Repository
public interface CursoRepository extends JpaRepository<Curso, Integer> {

  @Query("SELECT DISTINCT c FROM Curso c JOIN FETCH c.secciones s JOIN c.postulaciones p WHERE p.id = :idPostulacion")
  List<Curso> findByIdPostulacion(@Param("idPostulacion") Integer idPostulacion);

  boolean existsByCodigo(String codigo);
}
