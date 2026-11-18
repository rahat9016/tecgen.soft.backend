package com.tecgensoft.turf.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class SwaggerConfig {

  @Bean
  GroupedOpenApi publicApis() {
    return GroupedOpenApi.builder()
        .group("public")
        .displayName("Public APIs")
        .pathsToMatch("/api/v1/public/**")
        .build();
  }

  @Bean
  GroupedOpenApi internalApis() {
    return GroupedOpenApi.builder()
        .group("internal")
        .displayName("Organization APIs")
        .pathsToMatch("/api/v1/turf/**", "/api/v1/admin/turf/**")
        .build();
  }

  @Bean
  OpenAPI turfOpenApi(
      @Value("${turf.swagger.server-url:http://localhost:8081}") String serverUrl) {
    return new OpenAPI()
        .addServersItem(new io.swagger.v3.oas.models.servers.Server().url(serverUrl))
        .info(
            new Info()
                .title("Tecgen Soft Turf API")
                .description("Public booking APIs and authenticated organization management APIs")
                .version("v1.0.0"));
  }
}
