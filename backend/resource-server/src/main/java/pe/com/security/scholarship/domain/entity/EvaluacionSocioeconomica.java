package pe.com.security.scholarship.domain.entity;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import pe.com.security.scholarship.domain.enums.NivelSocioeconomico;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "evaluaciones_socioeconomicas")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class EvaluacionSocioeconomica {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eval_socio_seq")
  @SequenceGenerator(
          name = "eval_socio_seq",
          sequenceName = "evaluaciones_socioeconomicas_id_seq",
          allocationSize = 50
  )
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_estudiante", nullable = false)
  private Estudiante estudiante;

  private LocalDate fechaEvaluacion;
  private LocalDate fechaExpiracion;

  @Enumerated(EnumType.STRING)
  private NivelSocioeconomico nivelSocioeconomico;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  private Instant updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", nullable = false)
  private Empleado createdBy;
}
