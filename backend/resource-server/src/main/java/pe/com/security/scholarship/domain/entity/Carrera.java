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

import java.util.List;

@Entity
@Table(name = "carreras")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Carrera {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(length = 45, nullable = false)
  private String nombre;

  @Column(length = 6, nullable = false)
  private String codigoFacultad;

  @OneToMany(mappedBy = "carrera", cascade = CascadeType.ALL)
  private List<Estudiante> estudiantes;
}
