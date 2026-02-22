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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "estudiantes")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Estudiante {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true)
  private UUID idUsuario;

  @Column(length = 9, nullable = false, unique = true)
  private String codigoEstudiante;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_carrera", nullable = false)
  private Carrera carrera;

  @Column(length = 9)
  private String celular;

  @Column(length = 100)
  private String direccionDomicilio;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  private Instant updatedAt;

  @OneToMany(mappedBy = "estudiante", cascade = CascadeType.ALL)
  private List<PromedioPonderado> promediosPonderados;

  @OneToMany(mappedBy = "estudiante", cascade = CascadeType.ALL)
  private List<EvaluacionSocioeconomica> evaluacionesSocioeconomicas;

  @OneToMany(mappedBy = "estudiante", cascade = CascadeType.ALL)
  private List<Postulacion> postulaciones;
}
