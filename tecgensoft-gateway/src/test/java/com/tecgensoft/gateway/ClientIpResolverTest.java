package com.tecgensoft.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClientIpResolverTest {
    private final ClientIpResolver resolver = new ClientIpResolver(new GatewayProperties(
            URI.create("http://core"), URI.create("http://turf"), "key", List.of(), false,
            List.of("10.0.0.0/8"), List.of("198.51.100.7"), 1, 1, null));

    private String resolve(String peer, String... forwarded) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(peer);
        for (String value : forwarded) {
            request.addHeader("X-Forwarded-For", value);
        }
        return resolver.resolve(request).getHostAddress();
    }

    @Test
    void ignoresForwardedHeaderFromUntrustedPeers() {
        assertEquals("203.0.113.5", resolve("203.0.113.5", "1.2.3.4"));
    }

    @Test
    void usesTheLastUntrustedHopBehindATrustedProxy() {
        assertEquals("203.0.113.5", resolve("10.0.0.2", "1.2.3.4, 203.0.113.5"));
        assertEquals("203.0.113.5", resolve("10.0.0.2", "1.2.3.4", "203.0.113.5, 10.1.1.1"));
        assertEquals("10.0.0.2", resolve("10.0.0.2"));
        assertEquals("10.0.0.2", resolve("10.0.0.2", "garbage"));
    }

    @Test
    void recognisesSsrServersAndGroupsIpv6() throws Exception {
        assertTrue(resolver.isSsrClient(InetAddress.ofLiteral("198.51.100.7")));
        assertFalse(resolver.isSsrClient(InetAddress.ofLiteral("198.51.100.8")));
        assertEquals(ClientIpResolver.rateLimitKey(InetAddress.ofLiteral("2001:db8:1:2::1")),
                ClientIpResolver.rateLimitKey(InetAddress.ofLiteral("2001:db8:1:2:ffff::9")));
    }
}
