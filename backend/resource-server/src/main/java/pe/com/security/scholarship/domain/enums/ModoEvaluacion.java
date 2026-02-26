package pe.com.security.scholarship.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ModoEvaluacion {
  PROMEDIO_PONDERADO,
  SOCIOECONOMICO,
  MIXTO;

  @Override
  @JsonValue
  public String toString() {
    return this.name().replace("_", " ");
  }
}
