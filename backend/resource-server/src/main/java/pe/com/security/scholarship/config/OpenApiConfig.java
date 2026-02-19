package pe.com.security.scholarship.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
            .info(new Info()
                    .title("Scholarship Resource Server API")
                    .description("APIs de Negocio protegidas por JWT")
                    .version("1.0.0"))
            // Añadimos el requisito de seguridad global
            .addSecurityItem(new SecurityRequirement().addList("Bearer_Token"))
            .components(new Components()
                    .addSecuritySchemes("Bearer_Token",
                            new SecurityScheme()
                                    .name("Bearer_Token")
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT")
                                    .description("Copia el 'access_token' obtenido del Auth Server (Puerto 9000)")));
  }

  @Bean
  public GroupedOpenApi businessAPI() {
    return GroupedOpenApi.builder()
            .group("scholarship-business")
            .pathsToMatch("/api/**") // Tus rutas de negocio
            .build();
  }
}