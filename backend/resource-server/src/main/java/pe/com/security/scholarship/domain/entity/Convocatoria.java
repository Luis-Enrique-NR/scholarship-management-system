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
import jakarta.persistence.ManyToOne;
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
import pe.com.security.scholarship.domain.enums.EstadoConvocatoria;
import pe.com.security.scholarship.domain.enums.Mes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "convocatorias")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Convocatoria {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Mes mes;

  @Column(nullable = false)
  private LocalDate fechaInicio;

  @Column(nullable = false)
  private LocalDate fechaFin;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EstadoConvocatoria estado;

  @Column(nullable = false)
  private Integer cantidadVacantes;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  private Instant updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", nullable = false)
  private Empleado createdBy;

  @OneToMany(mappedBy = "convocatoria", cascade = CascadeType.ALL)
  private List<Postulacion> postulaciones;
}
