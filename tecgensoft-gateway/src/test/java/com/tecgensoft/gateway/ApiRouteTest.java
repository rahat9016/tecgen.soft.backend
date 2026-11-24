package com.tecgensoft.gateway;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiRouteTest {
    @Test
    void classifiesReachablePaths() {
        assertEquals(ApiRoute.PUBLIC_TENANT, ApiRoute.of("GET", "/api/v1/public/tenant").orElseThrow());
        assertEquals(ApiRoute.PUBLIC_TURF, ApiRoute.of("POST", "/api/v1/public/turf/holds").orElseThrow());
        assertEquals(ApiRoute.TURF_ADMIN, ApiRoute.of("GET",
                "/api/v1/turf/organizations/5f0c6f5e-1111-4111-8111-000000000000/courts").orElseThrow());
        assertEquals(ApiRoute.CORE, ApiRoute.of("POST", "/api/v1/auth/login").orElseThrow());
        assertEquals(ApiRoute.CORE, ApiRoute.of("GET", "/api/v1/organizations").orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/", "/internal/v1/tenants/resolve", "/INTERNAL/v1/access/check", "/error", "/actuator/health",
            "/v3/api-docs", "/swagger-ui/index.html", "/ws/info", "/api/v1/admin/turf/bookings",
            "/api/v1/authx/login", "/api/v1/public/other", "/api/v1/public/turf",
            "/api/v1/auth/../../../internal/v1/access/check", "/api/v1/auth/%2e%2e/%2e%2e/internal",
            "/api/v1/auth/..%2f..%2finternal", "/api/v1/auth/..;/internal", "/api/v1/auth;x=/login",
            "//internal/v1/tenants/resolve", "/api/v1//auth/login", "/api/v1/auth/./login",
            "/api/v1/auth\\..\\internal", "/api/v1/auth/login%00"
    })
    void refusesEverythingElse(String path) {
        assertTrue(ApiRoute.of("GET", path).isEmpty(), path);
    }

    @Test
    void tenantInformationIsReadOnly() {
        assertTrue(ApiRoute.of("POST", "/api/v1/public/tenant").isEmpty());
    }
}
