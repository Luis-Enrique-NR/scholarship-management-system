package pe.com.learning.security.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
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
                    .title("Learning API - Documentation")
                    .description("Servidor de Recursos y Autorización con Spring Boot 3 & OAuth2")
                    .version("1.0.0")
                    .contact(new Contact().name("Support Team").email("soporte@learning.com.pe")))
            // Configuramos el esquema de seguridad como OAuth2
            .addSecurityItem(new SecurityRequirement().addList("OAuth2_Security"))
            .components(new Components()
                    .addSecuritySchemes("OAuth2_Security",
                            new SecurityScheme()
                                    .name("OAuth2_Security")
                                    .type(SecurityScheme.Type.OAUTH2)
                                    .description("Autenticación basada en el flujo de Authorization Code")
                                    .flows(new OAuthFlows()
                                            .authorizationCode(new OAuthFlow()
                                                    .authorizationUrl("http://localhost:8080/oauth2/authorize")
                                                    .tokenUrl("http://localhost:8080/oauth2/token")
                                                    .scopes(new Scopes()
                                                            .addString("read", "Permiso de lectura")
                                                            .addString("write", "Permiso de escritura"))
                                            )
                                    )
                    ));
  }

  @Bean
  public GroupedOpenApi publicAPI() {
    return GroupedOpenApi.builder()
            .group("learning-api")
            .packagesToScan("pe.com.learning.security.controller")
            .pathsToMatch("/api/**") // Solo documentamos nuestras APIs de negocio
            .build();
  }
}