package pe.com.security.scholarship.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "matriculas")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Matricula {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_postulacion", nullable = false)
  private Postulacion postulacion;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private Instant fechaSolicitud;

  private Instant fechaMatricula;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_empleado")
  private Empleado empleado;

  @Column(length = 10, nullable = false)
  private String estado;

  @Column(columnDefinition = "NUMERIC(4,2)")
  private Double nota;

  @UpdateTimestamp
  private Instant updatedAt;
}
