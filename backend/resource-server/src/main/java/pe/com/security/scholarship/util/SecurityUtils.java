package pe.com.security.scholarship.util;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public class SecurityUtils {

  public static UUID getCurrentUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
      return UUID.fromString(jwt.getClaimAsString("uid"));
    }
    throw new RuntimeException("No se encontró un usuario autenticado");
  }
}
