package pe.com.security.scholarship.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "periodos_academicos")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PeriodoAcademico {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(length = 4, nullable = false, unique = true)
  private String periodo;

  @Column(nullable = false)
  private LocalDate fechaInicio;

  @Column(nullable = false)
  private LocalDate fechaFin;

  @OneToMany(mappedBy = "periodoAcademico", cascade = CascadeType.ALL)
  private List<PromedioPonderado> promediosPonderados;
}
