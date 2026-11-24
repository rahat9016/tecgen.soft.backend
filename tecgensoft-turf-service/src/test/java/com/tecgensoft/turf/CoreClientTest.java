package com.tecgensoft.turf;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import com.tecgensoft.turf.service.CoreClient;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class CoreClientTest {
  @Test
  void forwardsSeparateCredentialsAndFailsClosed() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    try {
      var org = UUID.randomUUID();
      var user = UUID.randomUUID();
      server.createContext(
          "/internal/v1/access/check",
          exchange -> {
            assertEquals("service-key", exchange.getRequestHeaders().getFirst("X-Service-Key"));
            assertEquals(
                "Bearer original-user-token",
                exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body =
                ("{\"allowed\":true,\"reasonCode\":\"ALLOWED\",\"userId\":\"" + user + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
          });
      server.createContext(
          "/internal/v1/tenants/resolve",
          exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
          });
      server.start();
      var client =
          new CoreClient("http://127.0.0.1:" + server.getAddress().getPort(), "service-key");
      assertEquals(
          user,
          client.check(
              org,
              "Bearer original-user-token",
              "TURF_COURT_READ"));
      assertThrows(ResponseStatusException.class, () -> client.resolve("example.test"));
      assertThrows(
          ResponseStatusException.class,
          () ->
              client.check(
                  org, null, "TURF_COURT_READ"));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void separatesUnknownInactiveForeignAndUnavailableTenants() throws Exception {
    var status = new java.util.concurrent.atomic.AtomicInteger(404);
    var payload = new java.util.concurrent.atomic.AtomicReference<String>("{}");
    var calls = new java.util.concurrent.atomic.AtomicInteger();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/internal/v1/tenants/resolve",
        exchange -> {
          calls.incrementAndGet();
          byte[] body = payload.get().getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(status.get(), body.length == 0 ? -1 : body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    try {
      var client = new CoreClient("http://127.0.0.1:" + server.getAddress().getPort(), "k");
      var org = UUID.randomUUID();
      assertEquals(404, failure(client));
      status.set(200);
      payload.set("");
      assertEquals(404, failure(client));
      payload.set(tenant(org, "HOTEL", true));
      assertEquals(404, failure(client));
      payload.set(tenant(org, "TURF", false));
      assertEquals(403, failure(client));
      status.set(500);
      assertEquals(503, failure(client));
      status.set(200);
      payload.set(tenant(org, "TURF", true));
      assertEquals(org, client.resolve("a.test").organizationId());
      int before = calls.get();
      assertEquals(org, client.resolve("a.test").organizationId());
      assertEquals(before, calls.get());
    } finally {
      server.stop(0);
    }
    var down = new CoreClient("http://127.0.0.1:1", "k");
    assertEquals(503, failure(down));
  }

  private static String tenant(UUID org, String product, boolean active) {
    return "{\"organizationId\":\"%s\",\"product\":\"%s\",\"active\":%s}"
        .formatted(org, product, active);
  }

  private static int failure(CoreClient client) {
    return assertThrows(ResponseStatusException.class, () -> client.resolve("a.test"))
        .getStatusCode()
        .value();
  }
}
