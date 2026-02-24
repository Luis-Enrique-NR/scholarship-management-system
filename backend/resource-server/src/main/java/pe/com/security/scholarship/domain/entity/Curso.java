package pe.com.security.scholarship.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import pe.com.security.scholarship.domain.enums.ModalidadCurso;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "cursos")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Curso {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(length = 90, nullable = false)
  private String nombre;

  @Column(length = 5, nullable = false)
  private String codigo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ModalidadCurso modalidad;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  private Instant updatedAt;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
          name = "cursos_postulacion",
          joinColumns = @JoinColumn(name = "id_curso"),
          inverseJoinColumns = @JoinColumn(name = "id_postulacion")
  )
  private Set<Postulacion> postulaciones;

  @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL)
  private List<Seccion> secciones;
}
