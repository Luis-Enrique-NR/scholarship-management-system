package pe.com.security.scholarship.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import pe.com.security.scholarship.domain.enums.DiaSemana;

import java.time.LocalTime;

@Entity
@Table(name = "horarios_seccion")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HorarioSeccion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_seccion", nullable = false)
  private Seccion seccion;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DiaSemana diaSemana;

  @Column(nullable = false)
  private LocalTime horaInicio;

  @Column(nullable = false)
  private LocalTime horaFin;
}
