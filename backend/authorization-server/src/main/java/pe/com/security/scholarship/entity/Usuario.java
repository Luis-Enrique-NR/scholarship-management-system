package pe.com.security.scholarship.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import pe.com.security.scholarship.entity.enums.AuthProvider;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 30)
  private String nombres;

  @Column(nullable = false, length = 30)
  private String apellidos;

  @Column(unique = true, nullable = false, length = 35)
  private String correo;

  private String password;

  @Column(nullable = false)
  private Boolean habilitado;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AuthProvider provider;

  @Column(length = 100)
  private String providerId;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  private Instant updatedAt;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
          name = "roles_usuarios",
          joinColumns = @JoinColumn(name = "id_usuario"),
          inverseJoinColumns = @JoinColumn(name = "id_rol")
  )
  private Collection<Rol> roles;
}
