package pe.com.security.scholarship.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "postulaciones")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Postulacion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_estudiante", nullable = false)
  private Estudiante estudiante;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_convocatoria", nullable = false)
  private Convocatoria convocatoria;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDate fechaPostulacion;

  @Column(columnDefinition = "NUMERIC(5,3)")
  private Double promedioGeneral;

  private Boolean aceptado;

  @OneToMany(mappedBy = "postulacion", cascade = CascadeType.ALL)
  private List<Matricula> matriculas;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
          name = "cursos_postulacion",
          joinColumns = @JoinColumn(name = "id_postulacion"),
          inverseJoinColumns = @JoinColumn(name = "id_curso")
  )
  private Set<Curso> cursos;
}
