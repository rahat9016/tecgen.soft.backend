package com.tecgensoft.gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "gateway.service-key=gateway-test-key",
        "gateway.platform-origins=https://admin.tecgensoft.com",
        "gateway.rate-limit.overall.requests=100000",
        "gateway.rate-limit.read.requests=100000",
        "gateway.rate-limit.booking.requests=100000",
        "gateway.rate-limit.admin.requests=100000",
        "gateway.rate-limit.hold.requests=3",
        "gateway.rate-limit.login.requests=2",
        "management.server.port=0"
})
@DirtiesContext
class GatewayIntegrationTest {
    private record Seen(String service, String method, String path, Map<String, List<String>> headers, byte[] body) {
        String header(String name) {
            return headers.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase(name))
                    .map(entry -> entry.getValue().getFirst()).findFirst().orElse(null);
        }
    }

    private static final List<Seen> SEEN = new CopyOnWriteArrayList<>();
    private static final HttpServer CORE = server("core");
    private static final HttpServer TURF = server("turf");
    private static final String A = "https://turf-a.com";
    private static final String PLATFORM = "https://admin.tecgensoft.com";
    private static final String ORG = UUID.randomUUID().toString();

    @LocalServerPort
    int port;
    @Autowired
    GatewayProperties properties;
    private final HttpClient http = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        registry.add("gateway.core-url", () -> "http://127.0.0.1:" + CORE.getAddress().getPort());
        registry.add("gateway.turf-url", () -> "http://127.0.0.1:" + TURF.getAddress().getPort());
    }

    private static HttpServer server(String name) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> handle(name, exchange));
            server.start();
            return server;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static void handle(String name, HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        String path = exchange.getRequestURI().toString();
        String answer = "{\"service\":\"" + name + "\"}";
        int status = 200;
        if (path.equals("/internal/v1/tenants/resolve")) {
            assertEquals("gateway-test-key", exchange.getRequestHeaders().getFirst("X-Service-Key"));
            String request = new String(body, StandardCharsets.UTF_8);
            if (request.contains("\"down.com\"")) {
                status = 500;
            } else if (request.contains("\"turf-a.com\"") || request.contains("\"turf-b.com\"")) {
                answer = "{\"organizationId\":\"" + ORG + "\",\"product\":\"TURF\",\"active\":true}";
            } else {
                status = 404;
            }
        } else {
            SEEN.add(new Seen(name, exchange.getRequestMethod(), path, Map.copyOf(exchange.getRequestHeaders()), body));
        }
        byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @AfterAll
    static void stop() {
        CORE.stop(0);
        TURF.stop(0);
    }

    @BeforeEach
    void reset() {
        SEEN.clear();
    }

    private HttpResponse<String> call(String method, String path, String... headers) throws Exception {
        return call(method, path, new byte[0], headers);
    }

    private HttpResponse<String> call(String method, String path, byte[] body, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method, body.length == 0 ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofByteArray(body));
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void siteOriginSelectsTheTenantAndBeatsAForgedHeader() throws Exception {
        var response = call("GET", "/api/v1/public/turf/courts?x=1", "Origin", A, "X-Tenant-Host", "turf-b.com");
        assertEquals(200, response.statusCode(), response.body());
        assertEquals(List.of(A), response.headers().allValues("Access-Control-Allow-Origin"));
        assertTrue(response.headers().firstValue("Access-Control-Allow-Credentials").isEmpty());
        assertEquals(List.of("no-store"), response.headers().allValues("Cache-Control"));
        assertTrue(String.join(",", response.headers().allValues("Vary")).contains("X-Tenant-Host"));
        Seen seen = SEEN.getFirst();
        assertEquals("turf", seen.service());
        assertEquals("/api/v1/public/turf/courts?x=1", seen.path());
        assertEquals("turf-a.com", seen.header("X-Tenant-Host"));
        assertNull(seen.header("Origin"));
        assertEquals(response.headers().firstValue("X-Request-Id").orElseThrow(), seen.header("X-Request-Id"));
    }

    @Test
    void trustHeadersNeverReachTheServices() throws Exception {
        call("GET", "/api/v1/public/turf/courts", "Origin", A, "X-Service-Key", "stolen", "Cookie", "a=b",
                "Forwarded", "for=1.1.1.1", "X-Forwarded-For", "1.1.1.1", "X-Forwarded-Host", "evil.com",
                "Authorization", "Bearer user");
        Seen seen = SEEN.getFirst();
        for (String name : List.of("X-Service-Key", "Cookie", "Forwarded", "X-Forwarded-For", "X-Forwarded-Host")) {
            assertNull(seen.header(name), name);
        }
        assertEquals("Bearer user", seen.header("Authorization"));
    }

    @Test
    void serversAndThePlatformNameTheTenant() throws Exception {
        assertEquals(200, call("GET", "/api/v1/public/tenant", "X-Tenant-Host", "TURF-B.com:443").statusCode());
        assertEquals("core", SEEN.getFirst().service());
        assertEquals("turf-b.com", SEEN.getFirst().header("X-Tenant-Host"));
        assertEquals(200, call("GET", "/api/v1/public/tenant", "Origin", PLATFORM,
                "X-Tenant-Host", "turf-a.com").statusCode());
        assertEquals(400, call("GET", "/api/v1/public/tenant").statusCode());
        assertEquals(400, call("GET", "/api/v1/public/tenant", "X-Tenant-Host", "a b").statusCode());
        assertEquals(400, call("GET", "/api/v1/public/tenant", "X-Tenant-Host", "turf-a.com",
                "X-Tenant-Host", "turf-b.com").statusCode());
        assertEquals(404, call("GET", "/api/v1/public/tenant", "X-Tenant-Host", "nobody.com").statusCode());
        assertEquals(503, call("GET", "/api/v1/public/tenant", "X-Tenant-Host", "down.com").statusCode());
        assertEquals(503, call("GET", "/api/v1/public/tenant", "Origin", "https://down.com").statusCode());
    }

    @Test
    void preflightIsAnsweredOnlyForKnownOrigins() throws Exception {
        String[] ask = {"Access-Control-Request-Method", "POST", "Access-Control-Request-Headers", "content-type"};
        var allowed = call("OPTIONS", "/api/v1/public/turf/holds", with(ask, "Origin", A));
        assertEquals(200, allowed.statusCode());
        assertEquals(A, allowed.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
        for (String origin : List.of("https://evil.com", "http://turf-a.com", "https://turf-a.com:8443",
                "https://turf-a.com.evil.com", "https://user@turf-a.com", "null")) {
            var response = call("OPTIONS", "/api/v1/public/turf/holds", with(ask, "Origin", origin));
            assertEquals(403, response.statusCode(), origin);
            assertTrue(response.headers().firstValue("Access-Control-Allow-Origin").isEmpty(), origin);
        }
        assertEquals(403, call("GET", "/api/v1/public/turf/courts", "Origin", A, "Origin", PLATFORM).statusCode());
        assertTrue(SEEN.isEmpty());
    }

    @Test
    void managementIsClosedToOrganizationSites() throws Exception {
        String path = "/api/v1/turf/organizations/" + ORG + "/bookings";
        assertEquals(403, call("GET", path, "Origin", A).statusCode());
        assertEquals(403, call("POST", "/api/v1/auth/refresh-token", "Origin", A).statusCode());
        assertTrue(SEEN.isEmpty());
        assertEquals(200, call("GET", path, "Origin", PLATFORM, "X-Tenant-Host", "turf-b.com").statusCode());
        assertEquals("turf", SEEN.getFirst().service());
        assertNull(SEEN.getFirst().header("X-Tenant-Host"));
        assertEquals(200, call("GET", "/api/v1/organizations", "Origin", PLATFORM).statusCode());
        assertEquals("core", SEEN.get(1).service());
    }

    @Test
    void internalPathsAreUnreachable() throws Exception {
        for (String path : List.of("/internal/v1/tenants/resolve", "/%69nternal/v1/tenants/resolve",
                "/api/v1/auth/..%2f..%2f..%2finternal/v1/access/check", "/api/v1/auth/%2e%2e/%2e%2e/internal",
                "/api/v1/auth/..;/..;/..;/internal/v1/access/check", "//internal/v1/tenants/resolve",
                "/actuator/health", "/v3/api-docs", "/error")) {
            int status = call("POST", path, "X-Service-Key", "gateway-test-key").statusCode();
            assertTrue(status == 404 || status == 400, path + " " + status);
        }
        assertTrue(SEEN.isEmpty());
    }

    @Test
    void holdsAndLoginsAreLimitedPerClient() throws Exception {
        List<Integer> holds = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            holds.add(call("POST", "/api/v1/public/turf/holds", "Origin", A).statusCode());
        }
        assertEquals(List.of(200, 200, 200, 429), holds);
        var limited = call("POST", "/api/v1/public/turf/holds/", "Origin", A);
        assertEquals(429, limited.statusCode());
        assertTrue(limited.headers().firstValue("Retry-After").isPresent());
        assertEquals(A, limited.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
        assertEquals(200, call("POST", "/api/v1/public/turf/holds", "Origin", "https://turf-b.com").statusCode());
        List<Integer> logins = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            logins.add(call("POST", "/api/v1/auth/login", "X-Forwarded-For", "9.9.9." + i).statusCode());
        }
        assertEquals(List.of(200, 200, 429), logins);
    }

    @Test
    void streamsUploadsAndLimitsBodySize() throws Exception {
        byte[] upload = new byte[10 * 1024 * 1024];
        new java.util.Random(1).nextBytes(upload);
        assertEquals(200, call("POST", "/api/v1/organizations", upload,
                "Content-Type", "multipart/form-data; boundary=x").statusCode());
        assertArrayEquals(upload, SEEN.getFirst().body());
        assertEquals("multipart/form-data; boundary=x", SEEN.getFirst().header("Content-Type"));
        assertEquals(413, call("POST", "/api/v1/public/turf/bookings", new byte[70_000], "Origin", A).statusCode());
        assertEquals(1, SEEN.size());
    }

    @Test
    void forwardsPatchBodiesAndErrors() throws Exception {
        byte[] body = "{\"secret\":\"s\"}".getBytes(StandardCharsets.UTF_8);
        assertEquals(200, call("PATCH", "/api/v1/public/turf/holds/" + ORG + "/release", body,
                "Origin", A, "Content-Type", "application/json").statusCode());
        assertEquals("PATCH", SEEN.getFirst().method());
        assertArrayEquals(body, SEEN.getFirst().body());
    }

    private static String[] with(String[] base, String name, String value) {
        List<String> all = new ArrayList<>(List.of(base));
        all.add(name);
        all.add(value);
        return all.toArray(String[]::new);
    }
}
