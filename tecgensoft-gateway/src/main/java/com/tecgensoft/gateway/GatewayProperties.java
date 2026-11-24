package com.tecgensoft.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@ConfigurationProperties("gateway")
public record GatewayProperties(
        URI coreUrl,
        URI turfUrl,
        String serviceKey,
        List<String> platformOrigins,
        boolean allowInsecureOrigins,
        List<String> trustedProxies,
        List<String> ssrClientIps,
        long maxBodyBytes,
        long maxSiteBodyBytes,
        RateLimit rateLimit
) {
    public GatewayProperties {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalArgumentException("gateway.service-key is required");
        }
        platformOrigins = clean(platformOrigins);
        trustedProxies = clean(trustedProxies);
        ssrClientIps = clean(ssrClientIps);
    }

    public record Limit(int requests, Duration per) {
    }

    public record RateLimit(boolean enabled, int ssrMultiplier, Limit overall, Limit read, Limit hold,
                            Limit booking, Limit login, Limit refresh, Limit admin) {
    }

    private static List<String> clean(List<String> values) {
        return values == null ? List.of()
                : values.stream().map(String::strip).filter(value -> !value.isEmpty()).toList();
    }
}
