package pe.com.learning.security.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import pe.com.learning.security.util.ApiResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void handle(HttpServletRequest request,
                     HttpServletResponse response,
                     AccessDeniedException accessDeniedException) throws IOException, ServletException {

    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403

    String mensaje = "No tienes los privilegios necesarios para esta acción.";

    // Mantener la lógica de auditoría
    if (accessDeniedException.getMessage().toLowerCase().contains("denyall")) {
      mensaje = "Acceso restringido por política de seguridad crítica.";
    }

    // Usar tu ApiResponse estándar
    ApiResponse<String> errorResponse = new ApiResponse<>(
            mensaje,
            "403",
            request.getRequestURI()
    );

    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
  }
}
