package pe.com.security.scholarship.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.com.security.scholarship.domain.entity.PromedioPonderado;

@Repository
public interface PromedioPonderadoRepository extends JpaRepository<PromedioPonderado, Integer> {

}
