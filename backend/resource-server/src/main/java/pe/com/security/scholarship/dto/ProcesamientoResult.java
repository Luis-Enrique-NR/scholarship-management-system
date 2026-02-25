package pe.com.security.scholarship.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProcesamientoResult {
  private int total;
  private int exitos;
  private int fallidos;
  private List<ErrorDetalle> errores;

  @Data
  @AllArgsConstructor
  public static class ErrorDetalle {
    private String identifier;
    private String mensaje;
  }
}