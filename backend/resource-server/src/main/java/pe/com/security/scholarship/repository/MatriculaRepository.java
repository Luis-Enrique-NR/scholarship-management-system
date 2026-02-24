package pe.com.security.scholarship.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.security.scholarship.domain.entity.Matricula;

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
}
