package pe.com.security.scholarship.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.security.scholarship.domain.entity.HorarioSeccion;

public interface HorarioSeccionRepository extends JpaRepository<HorarioSeccion, Integer> {
}
