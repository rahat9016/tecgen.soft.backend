package com.tecgensoft.gateway;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Finds the real client address. X-Forwarded-For counts only when a configured proxy sent it. */
@Component
public class ClientIpResolver {
    private final List<Cidr> trustedProxies;
    private final List<Cidr> ssrClients;

    public ClientIpResolver(GatewayProperties properties) {
        this.trustedProxies = properties.trustedProxies().stream().map(Cidr::parse).toList();
        this.ssrClients = properties.ssrClientIps().stream().map(Cidr::parse).toList();
    }

    public InetAddress resolve(HttpServletRequest request) {
        InetAddress peer = InetAddress.ofLiteral(request.getRemoteAddr());
        if (!matches(trustedProxies, peer)) {
            return peer;
        }
        List<String> hops = new ArrayList<>();
        for (String header : Collections.list(request.getHeaders("X-Forwarded-For"))) {
            hops.addAll(List.of(header.split(",")));
        }
        // Each proxy appends the address it saw, so walk back until the first untrusted hop.
        InetAddress client = peer;
        for (String hop : hops.reversed()) {
            try {
                client = InetAddress.ofLiteral(hop.strip());
            } catch (IllegalArgumentException ex) {
                return client;
            }
            if (!matches(trustedProxies, client)) {
                return client;
            }
        }
        return client;
    }

    public boolean isSsrClient(InetAddress address) {
        return matches(ssrClients, address);
    }

    /** One IPv6 customer usually owns a whole /64, so it shares one bucket. */
    public static String rateLimitKey(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            return address.getHostAddress();
        }
        return java.util.HexFormat.of().formatHex(bytes, 0, 8) + "/64";
    }

    private static boolean matches(List<Cidr> ranges, InetAddress address) {
        return ranges.stream().anyMatch(range -> range.contains(address));
    }

    record Cidr(byte[] network, int prefix) {
        static Cidr parse(String value) {
            int slash = value.indexOf('/');
            byte[] network = InetAddress.ofLiteral(slash < 0 ? value : value.substring(0, slash)).getAddress();
            int prefix = slash < 0 ? network.length * 8 : Integer.parseInt(value.substring(slash + 1));
            if (prefix < 0 || prefix > network.length * 8) {
                throw new IllegalArgumentException("Invalid address range: " + value);
            }
            return new Cidr(network, prefix);
        }

        boolean contains(InetAddress address) {
            byte[] bytes = address.getAddress();
            if (bytes.length != network.length) {
                return false;
            }
            for (int bit = 0; bit < prefix; bit++) {
                int mask = 0x80 >> (bit % 8);
                if ((bytes[bit / 8] & mask) != (network[bit / 8] & mask)) {
                    return false;
                }
            }
            return true;
        }
    }
}
