package com.tecgensoft.gateway;

import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

import static org.springframework.cloud.gateway.server.mvc.filter.AfterFilterFunctions.setResponseHeader;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/** EdgeFilter has already refused everything that {@link ApiRoute} does not list. */
@Configuration
class RouteConfig {

    @Bean
    RouterFunction<ServerResponse> routes(GatewayProperties properties) {
        return to(properties.turfUrl(), "/api/v1/public/turf/**")
                .and(to(properties.turfUrl(), "/api/v1/turf/**"))
                .and(to(properties.coreUrl(), "/api/v1/**"));
    }

    private static RouterFunction<ServerResponse> to(URI service, String pattern) {
        return route().route(path(pattern), http())
                .before(uri(service))
                .after(setResponseHeader("Cache-Control", "no-store"))
                .build();
    }

    @Bean
    RestClientCustomizer proxyClient() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .connectTimeout(Duration.ofSeconds(2))
                        .build());
        factory.setReadTimeout(Duration.ofSeconds(30));
        return builder -> builder.requestFactory(factory);
    }
}
