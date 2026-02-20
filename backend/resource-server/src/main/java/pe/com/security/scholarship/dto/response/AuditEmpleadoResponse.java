package pe.com.security.scholarship.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AuditEmpleadoResponse {
  private String codigo;
  private String nombreCompleto;
  private List<String> rol;
}
