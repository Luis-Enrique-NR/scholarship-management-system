package pe.com.security.scholarship.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.security.scholarship.domain.entity.Seccion;

import java.util.Optional;

@Repository
public interface SeccionRepository extends JpaRepository<Seccion, Integer> {

  @Query("SELECT s FROM Seccion s JOIN FETCH s.horarios JOIN FETCH s.curso WHERE s.id = :idSeccion")
  Optional<Seccion> findById(@Param("idSeccion") Integer idSeccion);

  @Query("SELECT s.vacantesDisponibles - (SELECT COUNT(m) FROM Matricula m WHERE m.seccion.id = :idSeccion AND m.estado = 'ACEPTADO') " +
          "FROM Seccion s WHERE s.id = :idSeccion")
  Integer getVacantesRestantes(@Param("idSeccion") Integer idSeccion);
}
