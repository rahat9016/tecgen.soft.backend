package com.familiemunshi.common.configs;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class InternalSecurityConfig {
  @Bean
  @Order(1)
  SecurityFilterChain internal(HttpSecurity http, @Value("${internal.service-key}") String key)
      throws Exception {
    if (key.length() < 32)
      throw new IllegalStateException(
          "Internal service credential must have at least 32 characters");
    return http.securityMatcher("/internal/**")
        .csrf(c -> c.disable())
        .sessionManagement(
            s ->
                s.sessionCreationPolicy(
                    org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a.anyRequest().authenticated())
        .addFilterBefore(
            new OncePerRequestFilter() {
              protected void doFilterInternal(
                  HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                  throws ServletException, IOException {
                String supplied = req.getHeader("X-Service-Key");
                if (supplied == null
                    || !MessageDigest.isEqual(
                        key.getBytes(StandardCharsets.UTF_8),
                        supplied.getBytes(StandardCharsets.UTF_8))) {
                  res.sendError(401, "Invalid service credential");
                  return;
                }
                SecurityContextHolder.getContext()
                    .setAuthentication(
                        new UsernamePasswordAuthenticationToken("turf-service", null, List.of()));
                chain.doFilter(req, res);
              }
            },
            UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
