package pe.com.learning.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.learning.security.entity.Usuario;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

  // El JOIN FETCH trae al usuario y sus roles en un solo SELECT
  @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.roles WHERE u.correo = :correo")
  Optional<Usuario> findByCorreoWithRoles(@Param("correo") String correo);

  Optional<Usuario> findByCorreo(String correo);

  Boolean existsByCorreo(String email);
}
