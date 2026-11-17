package com.tecgensoft.turf.service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CoreClient {
  private final RestClient client;

  public CoreClient(@Value("${core.url}") String url, @Value("${core.service-key}") String key) {
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
    factory.setReadTimeout(Duration.ofSeconds(5));
    client =
        RestClient.builder()
            .baseUrl(url)
            .requestFactory(factory)
            .defaultHeader("X-Service-Key", key)
            .build();
  }

  public record Tenant(UUID organizationId, String product, boolean active) {}

  public record Entitlements(
      UUID organizationId,
      UUID packageId,
      String packageCode,
      int version,
      String subscriptionStatus,
      boolean active,
      Set<String> features,
      Map<String, Integer> limits) {}

  public record Decision(boolean allowed, String reasonCode, UUID userId) {}

  public record Access(UUID organizationId, String product, String permissionCode) {}

  public Tenant resolve(String host) {
    try {
      var t =
          client
              .post()
              .uri("/internal/v1/tenants/resolve")
              .body(Map.of("hostname", host))
              .retrieve()
              .body(Tenant.class);
      if (t == null || t.organizationId() == null || !t.active() || !"TURF".equals(t.product()))
        throw new IllegalStateException();
      return t;
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Tenant cannot be verified");
    }
  }

  public UUID check(Tenant tenant, String authorization, String permission) {
    if (authorization == null || !authorization.startsWith("Bearer "))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token required");
    Decision d;
    try {
      d =
          client
              .post()
              .uri("/internal/v1/access/check")
              .header("Authorization", authorization)
              .body(new Access(tenant.organizationId(), "TURF", permission))
              .retrieve()
              .body(Decision.class);
    } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user access token");
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Authorization cannot be verified");
    }
    if (d == null || !d.allowed() || d.userId() == null)
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, d == null ? "Authorization denied" : d.reasonCode());
    return d.userId();
  }

  public Entitlements entitlements(UUID org) {
    try {
      var e =
          client
              .get()
              .uri("/internal/v1/organizations/{id}/entitlements?product=TURF", org)
              .retrieve()
              .body(Entitlements.class);
      if (e == null
          || !org.equals(e.organizationId())
          || !e.active()
          || e.features() == null
          || e.limits() == null) throw new IllegalStateException();
      return e;
    } catch (Exception ex) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Entitlements cannot be verified");
    }
  }
}
