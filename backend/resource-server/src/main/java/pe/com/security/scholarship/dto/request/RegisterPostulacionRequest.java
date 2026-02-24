package pe.com.security.scholarship.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.HashSet;
import java.util.List;

@Data
public class RegisterPostulacionRequest {
  @NotNull(message = "La lista de cursos es obligatoria")
  @Size(min = 1, max = 3, message = "Debe seleccionar al menos un curso y máximo tres")
  private List<Integer> idsCursos; // Usamos List para poder detectar si hay repetidos

  @JsonIgnore
  @AssertTrue(message = "No se permiten cursos duplicados")
  public boolean isIdsCursosUnicos() {
    if (idsCursos == null) return true;
    return idsCursos.size() == new HashSet<>(idsCursos).size();
  }

  @NotNull(message = "ID de convocatoria obligatorio")
  private Integer idConvocatoria;
}
