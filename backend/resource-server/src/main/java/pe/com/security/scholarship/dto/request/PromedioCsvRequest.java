package pe.com.security.scholarship.dto.request;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;
import pe.com.security.scholarship.dto.CsvImportRequest;

@Data
public class PromedioCsvRequest implements CsvImportRequest {
  @CsvBindByName(column = "codigo", required = true)
  private String codigo;

  @CsvBindByName(column = "ciclo", required = true)
  private Integer ciclo;

  @CsvBindByName(column = "promedio", required = true)
  private Double promedio;

  @Override
  public String getIdentifier() {
    return this.codigo;
  }
}