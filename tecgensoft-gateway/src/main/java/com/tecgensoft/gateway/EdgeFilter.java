package com.tecgensoft.gateway;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Decides, in this order: request ID, reachable path, overall client limit, browser origin and CORS,
 * organization, route limit. Only then is the request handed to the router with cleaned headers.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class EdgeFilter extends OncePerRequestFilter {
    static final String TENANT_HOST = "X-Tenant-Host";
    static final String REQUEST_ID = "X-Request-Id";

    private static final Logger log = LoggerFactory.getLogger("com.tecgensoft.gateway.access");
    private static final Pattern CLIENT_REQUEST_ID = Pattern.compile("[A-Za-z0-9-]{8,64}");
    private static final Set<String> METHODS =
            Set.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    private final GatewayProperties properties;
    private final ClientIpResolver clientIps;
    private final RateLimiter limiter;
    private final TenantDirectory tenants;
    private final DefaultCorsProcessor cors = new DefaultCorsProcessor();

    public EdgeFilter(GatewayProperties properties, ClientIpResolver clientIps, RateLimiter limiter,
                      TenantDirectory tenants) {
        this.properties = properties;
        this.clientIps = clientIps;
        this.limiter = limiter;
        this.tenants = tenants;
    }

    /** Who a browser request came from. Non-browser clients send no Origin. */
    private sealed interface Caller {
        record NoOrigin() implements Caller {
        }

        record Platform() implements Caller {
        }

        record Site(String host) implements Caller {
        }
    }

    private static class Rejection extends Exception {
        private final int status;

        private Rejection(int status, String message) {
            super(message, null, false, false);
            this.status = status;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long started = System.nanoTime();
        String requestId = requestId(request);
        response.setHeader(REQUEST_ID, requestId);
        String tenantHost = null;
        InetAddress client = clientIps.resolve(request);
        try {
            ApiRoute route = METHODS.contains(request.getMethod())
                    ? ApiRoute.of(request.getMethod(), request.getRequestURI()).orElse(null) : null;
            if (route == null) {
                throw new Rejection(404, "Not found");
            }
            checkBody(request, route);
            boolean ssr = clientIps.isSsrClient(client);
            int multiplier = ssr ? properties.rateLimit().ssrMultiplier() : 1;
            String clientKey = ClientIpResolver.rateLimitKey(client);
            limit(response, "overall|" + clientKey, properties.rateLimit().overall(), multiplier);

            Caller caller = caller(request);
            boolean preflight = CorsUtils.isPreFlightRequest(request);
            if (!(caller instanceof Caller.NoOrigin)) {
                if (caller instanceof Caller.Site && !route.tenantSite()) {
                    throw new Rejection(403, "Origin not allowed");
                }
                if (!cors.processRequest(corsConfiguration(request), request, response)) {
                    return;
                }
            }
            response.addHeader(HttpHeaders.VARY, TENANT_HOST);
            if (preflight) {
                return;
            }
            if (route.tenantSite()) {
                tenantHost = tenantHost(request, caller);
            }
            routeLimit(request, response, route, clientKey, tenantHost, multiplier);
            forward(new SanitizedRequest(request, requestId, tenantHost), response, chain);
        } catch (Rejection rejection) {
            reject(response, rejection.status, rejection.getMessage());
        } finally {
            log.info("{} {} {} {}ms client={} tenant={} id={}", request.getMethod(),
                    loggable(request.getRequestURI()), response.getStatus(),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    client.getHostAddress(), tenantHost, requestId);
        }
    }

    /** Sites only send small JSON documents; uploads exist on the management routes alone. */
    private void checkBody(HttpServletRequest request, ApiRoute route) throws Rejection {
        if (request.getHeader(HttpHeaders.TRANSFER_ENCODING) != null) {
            throw new Rejection(411, "Content-Length is required");
        }
        long limit = route.tenantSite() ? properties.maxSiteBodyBytes() : properties.maxBodyBytes();
        if (request.getContentLengthLong() > limit) {
            throw new Rejection(413, "Request is too large");
        }
    }

    private static String requestId(HttpServletRequest request) {
        String supplied = request.getHeader(REQUEST_ID);
        return supplied != null && CLIENT_REQUEST_ID.matcher(supplied).matches()
                ? supplied : UUID.randomUUID().toString();
    }

    private Caller caller(HttpServletRequest request) throws Rejection {
        List<String> origins = Collections.list(request.getHeaders(HttpHeaders.ORIGIN));
        if (origins.isEmpty()) {
            return new Caller.NoOrigin();
        }
        String origin = origins.getFirst();
        if (origins.size() == 1) {
            if (properties.platformOrigins().contains(origin.toLowerCase(Locale.ROOT))) {
                return new Caller.Platform();
            }
            String host = siteHost(origin).orElse(null);
            if (host != null && find(host).isPresent()) {
                return new Caller.Site(host);
            }
        }
        throw new Rejection(403, "Origin not allowed");
    }

    private Optional<String> siteHost(String origin) {
        URI uri;
        try {
            uri = URI.create(origin);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        if (uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawQuery() != null
                || uri.getRawFragment() != null || !uri.getRawPath().isEmpty()) {
            return Optional.empty();
        }
        boolean secure = "https".equalsIgnoreCase(uri.getScheme()) && uri.getPort() == -1;
        boolean insecure = properties.allowInsecureOrigins()
                && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()));
        return secure || insecure ? TenantDirectory.normalize(uri.getHost()) : Optional.empty();
    }

    /** The organization site's own origin always wins over a client supplied X-Tenant-Host. */
    private String tenantHost(HttpServletRequest request, Caller caller) throws Rejection {
        if (caller instanceof Caller.Site site) {
            return site.host();
        }
        List<String> values = Collections.list(request.getHeaders(TENANT_HOST));
        String host = values.size() == 1 ? TenantDirectory.normalize(values.getFirst()).orElse(null) : null;
        if (host == null) {
            throw new Rejection(400, "A single valid X-Tenant-Host header is required");
        }
        if (find(host).isEmpty()) {
            throw new Rejection(404, "Organization not found");
        }
        return host;
    }

    private Optional<TenantDirectory.Tenant> find(String host) throws Rejection {
        try {
            return tenants.find(host);
        } catch (TenantDirectory.UnavailableException ex) {
            log.warn("Tenant lookup failed: {}", String.valueOf(ex.getCause()));
            throw new Rejection(503, "Service temporarily unavailable");
        }
    }

    private static CorsConfiguration corsConfiguration(HttpServletRequest request) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(request.getHeader(HttpHeaders.ORIGIN)));
        configuration.setAllowedMethods(List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept",
                "X-Organization-Id", TENANT_HOST, REQUEST_ID));
        configuration.setExposedHeaders(List.of(REQUEST_ID, HttpHeaders.RETRY_AFTER));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(Duration.ofMinutes(10));
        return configuration;
    }

    private void routeLimit(HttpServletRequest request, HttpServletResponse response, ApiRoute route,
                            String clientKey, String tenantHost, int multiplier) throws Rejection {
        GatewayProperties.RateLimit limits = properties.rateLimit();
        String path = request.getRequestURI();
        String trimmed = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        boolean read = "GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod());
        String name;
        GatewayProperties.Limit limit;
        if (route.tenantSite()) {
            if (read) {
                name = "read";
                limit = limits.read();
            } else if (trimmed.endsWith("/holds")) {
                name = "hold";
                limit = limits.hold();
            } else {
                name = "booking";
                limit = limits.booking();
            }
        } else if (trimmed.equals("/api/v1/auth/refresh-token")) {
            name = "refresh";
            limit = limits.refresh();
        } else if (trimmed.startsWith("/api/v1/auth/")) {
            // Organization servers must not get a larger password-guessing budget.
            limit(response, "login|" + clientKey, limits.login(), 1);
            return;
        } else {
            name = "admin";
            limit = limits.admin();
        }
        limit(response, name + "|" + clientKey + "|" + tenantHost, limit, multiplier);
    }

    private void limit(HttpServletResponse response, String key, GatewayProperties.Limit limit, int multiplier)
            throws Rejection {
        if (!properties.rateLimit().enabled()) {
            return;
        }
        long wait = limiter.acquire(key, limit, multiplier);
        if (wait > 0) {
            response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(wait));
            throw new Rejection(429, "Too many requests");
        }
    }

    private void forward(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException, Rejection {
        try {
            chain.doFilter(request, response);
        } catch (ServletException | ResourceAccessException ex) {
            ResourceAccessException unreachable = unreachable(ex);
            if (unreachable == null || response.isCommitted()) {
                throw ex;
            }
            boolean timeout = unreachable.getCause() instanceof java.net.http.HttpTimeoutException
                    && !(unreachable.getCause() instanceof java.net.http.HttpConnectTimeoutException);
            log.warn("Service unreachable: {}", String.valueOf(unreachable.getCause()));
            throw timeout ? new Rejection(504, "Service did not answer in time")
                    : new Rejection(503, "Service temporarily unavailable");
        }
    }

    private static ResourceAccessException unreachable(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof ResourceAccessException found) {
                return found;
            }
        }
        return null;
    }

    private static void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setContentType("application/json");
        response.getOutputStream().write(
                ("{\"success\":false,\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8));
    }

    private static String loggable(String path) {
        String safe = path.replaceAll("[^A-Za-z0-9/_.%-]", "?");
        return safe.length() > 200 ? safe.substring(0, 200) : safe;
    }
}
