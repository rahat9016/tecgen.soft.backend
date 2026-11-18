package com.tecgensoft.turf.common;

import java.util.Map;
import org.springframework.context.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Configuration
public class WebConfig {
  @Bean
  SecurityFilterChain security(
      HttpSecurity http, org.springframework.web.cors.CorsConfigurationSource cors)
      throws Exception {
    return http.cors(c -> c.configurationSource(cors))
        .csrf(c -> c.disable())
        .sessionManagement(
            s ->
                s.sessionCreationPolicy(
                    org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers("/api/v1/turf/**", "/api/v1/public/turf/**", "/api/v1/admin/turf/**")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        .build();
  }

  @Bean
  org.springframework.web.cors.CorsConfigurationSource cors(
      @org.springframework.beans.factory.annotation.Value(
              "${cors.allowed-origins:http://localhost:3000}")
          String origins) {
    var config = new org.springframework.web.cors.CorsConfiguration();
    config.setAllowedOrigins(
        java.util.Arrays.stream(origins.split(",")).map(String::strip).toList());
    config.setAllowedMethods(java.util.List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type"));
    var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @RestControllerAdvice
  public static class Errors {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException ex) {
      return ResponseEntity.status(ex.getStatusCode())
          .body(
              Map.of(
                  "success",
                  false,
                  "message",
                  ex.getReason() == null ? "Request failed" : ex.getReason()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict() {
      return ResponseEntity.status(409)
          .body(
              Map.of(
                  "success",
                  false,
                  "message",
                  "Operation conflicts with existing data or occupancy"));
    }

    @ExceptionHandler({
      MethodArgumentNotValidException.class,
      IllegalArgumentException.class,
      jakarta.validation.ConstraintViolationException.class,
      org.springframework.web.method.annotation.HandlerMethodValidationException.class,
      org.springframework.http.converter.HttpMessageNotReadableException.class
    })
    ResponseEntity<?> validation() {
      return ResponseEntity.badRequest()
          .body(Map.of("success", false, "message", "Invalid request"));
    }
  }
}
