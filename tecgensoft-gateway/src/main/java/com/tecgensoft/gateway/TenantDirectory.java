package com.tecgensoft.gateway;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/** Looks up verified organization hostnames in Core. */
@Component
public class TenantDirectory {
    private static final Pattern HOST =
            Pattern.compile("(?=^.{1,253}$)^[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?$");
    private static final long FOUND_NANOS = Duration.ofSeconds(60).toNanos();
    private static final long UNKNOWN_NANOS = Duration.ofSeconds(15).toNanos();

    private final RestClient core;
    private final Cache<String, Optional<Tenant>> cache = Caffeine.newBuilder()
            .maximumSize(20_000)
            .expireAfter(Expiry.<String, Optional<Tenant>>writing(
                    (host, tenant) -> Duration.ofNanos(tenant.isPresent() ? FOUND_NANOS : UNKNOWN_NANOS)))
            .build();

    public record Tenant(UUID organizationId, String product, boolean active) {
    }

    /** Core did not answer, so nothing is known about the hostname. Never cached. */
    public static class UnavailableException extends RuntimeException {
        public UnavailableException(Throwable cause) {
            super(cause);
        }
    }

    public TenantDirectory(GatewayProperties properties) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.core = RestClient.builder().baseUrl(properties.coreUrl()).requestFactory(factory)
                .defaultHeader("X-Service-Key", properties.serviceKey()).build();
    }

    /** Lower-cased hostname without port, or empty when it is not a valid hostname. */
    public static Optional<String> normalize(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String host = value.strip().toLowerCase(Locale.ROOT);
        int port = host.indexOf(':');
        if (port >= 0) {
            host = host.substring(0, port);
        }
        return HOST.matcher(host).matches() && !host.contains("..") ? Optional.of(host) : Optional.empty();
    }

    public Optional<Tenant> find(String host) {
        return cache.get(host, this::load);
    }

    private Optional<Tenant> load(String host) {
        try {
            Tenant tenant = core.post().uri("/internal/v1/tenants/resolve")
                    .body(Map.of("hostname", host)).retrieve().body(Tenant.class);
            return Optional.ofNullable(tenant).filter(found -> found.organizationId() != null);
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (Exception ex) {
            throw new UnavailableException(ex);
        }
    }
}
