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
              new CoreClient.Tenant(org, "TURF", true),
              "Bearer original-user-token",
              "TURF_COURT_READ"));
      assertThrows(ResponseStatusException.class, () -> client.resolve("example.test"));
      assertThrows(
          ResponseStatusException.class,
          () ->
              client.check(
                  new CoreClient.Tenant(org, "TURF", true), null, "TURF_COURT_READ"));
    } finally {
      server.stop(0);
    }
  }
}
