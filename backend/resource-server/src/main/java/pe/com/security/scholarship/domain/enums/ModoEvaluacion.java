package pe.com.security.scholarship.domain.enums;

public enum ModoEvaluacion {
  PROMEDIO_PONDERADO,
  SOCIOECONOMICO,
  MIXTO;

  @Override
  public String toString() {
    return this.name().replace("_", " ");
  }
}
