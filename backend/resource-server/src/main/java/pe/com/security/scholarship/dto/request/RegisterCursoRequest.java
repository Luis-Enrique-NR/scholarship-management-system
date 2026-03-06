package pe.com.security.scholarship.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import pe.com.security.scholarship.domain.enums.ModalidadCurso;

import java.util.List;

@Data
public class RegisterCursoRequest {
  @NotNull(message = "El código del curso es obligatorio")
  @Size(min = 5, max = 5, message = "El código es de 5 caracteres")
  private String codigo;
  @NotNull(message = "El nombre del curso es obligatorio")
  private String nombre;
  @NotNull(message = "La modalidad del curso es obligatoria")
  private ModalidadCurso modalidadCurso;
  @Valid
  private List<RegisterSeccionRequest> secciones;
}
