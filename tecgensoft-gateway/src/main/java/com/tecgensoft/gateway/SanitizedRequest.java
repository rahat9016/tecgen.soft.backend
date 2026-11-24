package com.tecgensoft.gateway;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The request as the services see it. Headers that carry trust are removed and replaced by values
 * the gateway verified. Without Origin the services' own CORS rules do not apply a second time.
 */
class SanitizedRequest extends HttpServletRequestWrapper {
    private final Map<String, String> replaced = new LinkedHashMap<>();

    SanitizedRequest(HttpServletRequest request, String requestId, String tenantHost) {
        super(request);
        replaced.put("x-request-id", requestId);
        if (tenantHost != null) {
            replaced.put("x-tenant-host", tenantHost);
        }
    }

    private static boolean removed(String name) {
        return name.startsWith("x-forwarded-") || switch (name) {
            case "origin", "cookie", "forwarded", "x-real-ip", "x-service-key", "x-tenant-host",
                 "x-request-id", "x-http-method-override" -> true;
            default -> false;
        };
    }

    @Override
    public String getHeader(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        return removed(key) ? replaced.get(key) : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        if (!removed(key)) {
            return super.getHeaders(name);
        }
        String value = replaced.get(key);
        return Collections.enumeration(value == null ? List.of() : List.of(value));
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> names = new java.util.ArrayList<>(Collections.list(super.getHeaderNames()).stream()
                .filter(name -> !removed(name.toLowerCase(Locale.ROOT))).toList());
        names.addAll(replaced.keySet());
        return Collections.enumeration(names);
    }
}
