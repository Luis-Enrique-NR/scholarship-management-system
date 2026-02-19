package pe.com.security.scholarship.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;

@Entity
@Table(name = "roles")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Rol {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(nullable = false, length = 50)
  private String nombre;

  @Column(nullable = false, length = 80)
  private String descripcion;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
          name = "roles_usuarios",
          joinColumns = @JoinColumn(name = "id_rol"),
          inverseJoinColumns = @JoinColumn(name = "id_usuario")
  )
  private Collection<Usuario> usuarios;

}
