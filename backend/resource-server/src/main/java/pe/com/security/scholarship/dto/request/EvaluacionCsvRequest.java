package pe.com.security.scholarship.dto.request;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import lombok.Data;
import pe.com.security.scholarship.dto.CsvImportRequest;

import java.time.LocalDate;

@Data
public class EvaluacionCsvRequest implements CsvImportRequest{
  @CsvBindByName(column = "codigo", required = true)
  private String codigo;

  @CsvBindByName(column = "nivel", required = true)
  private String nivel;

  @CsvBindByName(column = "fecha_evaluacion", required = true)
  @CsvDate("dd-MM-yyyy")
  private LocalDate fechaEvaluacion;

  @Override
  public String getIdentifier() {
    return this.codigo;
  }
}
