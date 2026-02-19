package pe.com.security.scholarship.security.model;

import java.util.UUID;

public interface BaseUser {
  UUID getUsuarioId();
  String getNombreCompleto();
}