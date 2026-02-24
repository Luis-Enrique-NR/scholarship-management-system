package pe.com.security.scholarship.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.security.scholarship.domain.entity.Postulacion;

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
}
