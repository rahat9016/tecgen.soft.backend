package com.tecgensoft.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "gateway.service-key=k", "gateway.core-url=http://127.0.0.1:1", "gateway.turf-url=http://127.0.0.1:1",
        "management.server.port=0"
})
class GatewayUnavailableServiceTest {
    @LocalServerPort
    int port;

    @Test
    void answersServiceUnavailable() throws Exception {
        try (HttpClient http = HttpClient.newHttpClient()) {
            var response = http.send(HttpRequest.newBuilder(
                            URI.create("http://127.0.0.1:" + port + "/api/v1/organizations"))
                    .header("Origin", "https://admin.tecgensoft.com").build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(503, response.statusCode(), response.body());
            assertTrue(response.body().contains("\"success\":false"));
            assertEquals("https://admin.tecgensoft.com",
                    response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
        }
    }
}
