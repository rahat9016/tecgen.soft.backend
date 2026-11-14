package com.familiemunshi.common.configs;

import static com.familiemunshi.common.utils.Constants.*;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class LocalSwaggerConfig {
  @Bean
  public GroupedOpenApi publicApis() {
    return GroupedOpenApi.builder().group("Public APIs").pathsToMatch(API_BASE + "/**").build();
  }

  @Bean
  public GroupedOpenApi internalApis() {
    return GroupedOpenApi.builder().group("Internal APIs").pathsToMatch("/internal/v1/**").build();
  }

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title(APP_NAME)
                .description("Tecgen Soft Core API Documentation")
                .version("v1.0.0"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }
}
