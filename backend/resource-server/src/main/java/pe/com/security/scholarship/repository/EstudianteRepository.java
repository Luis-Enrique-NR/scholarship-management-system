package pe.com.security.scholarship.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.security.scholarship.domain.entity.Estudiante;
import pe.com.security.scholarship.dto.projection.IdentificacionEstudianteProjection;

import java.util.Optional;
import java.util.UUID;

public interface EstudianteRepository extends JpaRepository<Estudiante, UUID> {

  @Query("SELECT e FROM Estudiante e WHERE e.idUsuario = :idUsuario")
  Optional<Estudiante> findByIdUsuario(@Param("idUsuario") UUID idUsuario);

  @Query("SELECT e FROM Estudiante e WHERE e.codigoEstudiante = :codigo")
  Optional<Estudiante> findByCodigo(@Param("codigo") String codigo);

  @Query(value = """
        SELECT
            e.codigo_estudiante as codigo,
            CONCAT(u.nombres, ' ', u.apellidos) as nombreCompleto
        FROM estudiantes e
        INNER JOIN usuarios u ON u.id = e.id_usuario
        WHERE e.id = :idEstudiante
  """, nativeQuery = true)
  Optional<IdentificacionEstudianteProjection> findDatosEstudiante(@Param("idEstudiante") UUID idEstudiante);
}
