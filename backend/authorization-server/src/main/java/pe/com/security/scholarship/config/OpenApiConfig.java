package pe.com.security.scholarship.config;

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
                    .title("Scholarship Auth Server API")
                    .description("Servidor de Identidad - Obtén aquí tu token para el Resource Server")
                    .version("1.0.0"))
            // El SecurityRequirement activa el candado globalmente
            .addSecurityItem(new SecurityRequirement().addList("OAuth2_Auth_Code"))
            .components(new Components()
                    .addSecuritySchemes("OAuth2_Auth_Code",
                            new SecurityScheme()
                                    .name("OAuth2_Auth_Code")
                                    .type(SecurityScheme.Type.OAUTH2)
                                    .description("Flujo de código de autorización")
                                    .flows(new OAuthFlows()
                                            .authorizationCode(new OAuthFlow()
                                                    .authorizationUrl("http://localhost:9000/oauth2/authorize")
                                                    .tokenUrl("http://localhost:9000/oauth2/token")
                                                    .scopes(new Scopes()
                                                            .addString("openid", "Información de identidad")
                                                            .addString("read", "Acceso de lectura")
                                                            .addString("write", "Acceso de escritura"))
                                            )
                                    )
                    ));
  }

  @Bean
  public GroupedOpenApi authAPI() {
    return GroupedOpenApi.builder()
            .group("scholarship-auth")
            // Aquí escaneamos los controladores de autenticación
            .pathsToMatch("/api/v1/auth/**")
            .build();
  }
}