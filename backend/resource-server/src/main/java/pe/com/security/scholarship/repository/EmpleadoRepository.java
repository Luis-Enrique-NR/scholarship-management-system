package pe.com.security.scholarship.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.security.scholarship.entity.Empleado;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, UUID> {

  @Query("SELECT e FROM Empleado e WHERE e.idUsuario = :idUsuario")
  Optional<Empleado> findByIdUsuario(@Param("idUsuario") UUID idUsuario);
}
