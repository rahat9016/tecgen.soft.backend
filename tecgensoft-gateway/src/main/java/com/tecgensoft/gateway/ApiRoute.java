package com.tecgensoft.gateway;

import java.util.Optional;
import java.util.regex.Pattern;

/** The complete set of paths reachable from outside. Everything else is 404. */
public enum ApiRoute {
    /** Organization site information, served by Core. */
    PUBLIC_TENANT(true),
    /** Organization site API, served by Turf. */
    PUBLIC_TURF(true),
    /** Organization management, served by Turf; the organization is in the path. */
    TURF_ADMIN(false),
    /** Accounts and platform management, served by Core. */
    CORE(false);

    private static final Pattern SAFE_PATH = Pattern.compile("(/[A-Za-z0-9_-][A-Za-z0-9_.-]*)+/?");
    private static final Pattern PUBLIC_TURF_PATH = Pattern.compile("/api/v1/public/turf/.+");
    private static final Pattern TURF_ADMIN_PATH = Pattern.compile("/api/v1/turf/organizations/.+");
    private static final Pattern CORE_PATH =
            Pattern.compile("/api/v1/(auth|users|organizations|permissions|system)(/.*)?");

    private final boolean tenantSite;

    ApiRoute(boolean tenantSite) {
        this.tenantSite = tenantSite;
    }

    /** Whether the route serves an organization's site and therefore needs its hostname. */
    public boolean tenantSite() {
        return tenantSite;
    }

    /**
     * Classifies the raw, undecoded path. No API path needs percent-encoding, matrix parameters,
     * empty or dot segments, so those are refused rather than normalized.
     */
    public static Optional<ApiRoute> of(String method, String rawPath) {
        if (rawPath == null || !SAFE_PATH.matcher(rawPath).matches()) {
            return Optional.empty();
        }
        if (rawPath.equals("/api/v1/public/tenant")) {
            return switch (method) {
                case "GET", "HEAD", "OPTIONS" -> Optional.of(PUBLIC_TENANT);
                default -> Optional.empty();
            };
        }
        if (PUBLIC_TURF_PATH.matcher(rawPath).matches()) {
            return Optional.of(PUBLIC_TURF);
        }
        if (TURF_ADMIN_PATH.matcher(rawPath).matches()) {
            return Optional.of(TURF_ADMIN);
        }
        return CORE_PATH.matcher(rawPath).matches() ? Optional.of(CORE) : Optional.empty();
    }
}
