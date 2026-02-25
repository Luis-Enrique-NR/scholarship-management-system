package pe.com.security.scholarship.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

@Configuration
@Profile("!test")
public class WebConfig {
  @Bean
  public StandardServletMultipartResolver multipartResolver() {
    StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
    resolver.setResolveLazily(true); // Evita que explote en el filtro de entrada
    return resolver;
  }
}
