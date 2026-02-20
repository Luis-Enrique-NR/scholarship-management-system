package pe.com.security.scholarship.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pe.com.security.scholarship.entity.Convocatoria;

import java.time.LocalDate;

@Repository
public interface ConvocatoriaRepository extends JpaRepository<Convocatoria, Integer> {

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
}
