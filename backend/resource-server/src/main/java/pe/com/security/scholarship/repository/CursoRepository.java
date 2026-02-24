package pe.com.security.scholarship.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.security.scholarship.domain.entity.Curso;

public interface CursoRepository extends JpaRepository<Curso, Integer> {
}
