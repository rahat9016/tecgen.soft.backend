package com.tecgensoft.turf.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CoreClient {
  private final RestClient client;
  // Only successful answers are cached. A suspension therefore takes effect within the
  // entitlement lifetime, because every request also checks entitlements.
  private final Cache<String, Tenant> tenants =
      Caffeine.newBuilder().maximumSize(10_000).expireAfterWrite(Duration.ofSeconds(60)).build();
  private final Cache<UUID, Entitlements> entitlements =
      Caffeine.newBuilder().maximumSize(10_000).expireAfterWrite(Duration.ofSeconds(30)).build();

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
      String product,
      String productCode,
      String subscriptionStatus,
      boolean active,
      boolean isActive,
      boolean isTrial,
      long trialDaysRemaining,
      Map<String, Object> features,
      Map<String, Integer> limits) {}

  public record Decision(boolean allowed, String reasonCode, UUID userId) {}

  public record Access(UUID organizationId, String product, String permissionCode) {}

  public Tenant resolve(String host) {
    Tenant cached = tenants.getIfPresent(host);
    if (cached != null) return cached;
    Tenant t;
    try {
      t =
          client
              .post()
              .uri("/internal/v1/tenants/resolve")
              .body(Map.of("hostname", host))
              .retrieve()
              .body(Tenant.class);
    } catch (HttpClientErrorException.NotFound e) {
      throw unknownTenant();
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Tenant cannot be verified");
    }
    // Core versions before the 404 answer reported an unknown hostname as an empty 200.
    if (t == null || t.organizationId() == null || !"TURF".equals(t.product()))
      throw unknownTenant();
    if (!t.active())
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Organization is not active");
    tenants.put(host, t);
    return t;
  }

  private static ResponseStatusException unknownTenant() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found");
  }

  /** Never cached: a revoked token, membership or permission must take effect immediately. */
  public UUID check(UUID organizationId, String authorization, String permission) {
    if (authorization == null || !authorization.startsWith("Bearer "))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token required");
    Decision d;
    try {
      d =
          client
              .post()
              .uri("/internal/v1/access/check")
              .header("Authorization", authorization)
              .body(new Access(organizationId, "TURF", permission))
              .retrieve()
              .body(Decision.class);
    } catch (HttpClientErrorException.Unauthorized e) {
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
    Entitlements found = entitlements.getIfPresent(org);
    if (found == null) {
      found = fetchEntitlements(org);
      if (found.active()) entitlements.put(org, found);
    }
    if (!found.active())
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Organization has no active TURF subscription: " + found.subscriptionStatus());
    return found;
  }

  private Entitlements fetchEntitlements(UUID org) {
    Entitlements fetched;
    try {
      fetched =
          client
              .get()
              .uri("/internal/v1/organizations/{id}/entitlements?product=TURF", org)
              .retrieve()
              .body(Entitlements.class);
    } catch (Exception ex) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Entitlements cannot be verified");
    }
    if (fetched == null
        || !org.equals(fetched.organizationId())
        || fetched.features() == null
        || fetched.limits() == null)
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Entitlements cannot be verified");
    return fetched;
  }
}
