package pe.com.security.scholarship.dto.request;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;
import pe.com.security.scholarship.dto.CsvImportRequest;

@Data
public class NotaCsvRequest implements CsvImportRequest {
  @CsvBindByName(column = "codigo", required = true)
  private String codigo;
  @CsvBindByName(column = "nota", required = true)
  private Double nota;

  @Override
  public String getIdentifier() {
    return this.codigo;
  }
}
