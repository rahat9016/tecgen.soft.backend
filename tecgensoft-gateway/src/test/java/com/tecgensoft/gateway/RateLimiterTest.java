package com.tecgensoft.gateway;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {
    @Test
    void refillsOverTimeAndSeparatesKeys() {
        AtomicLong now = new AtomicLong();
        RateLimiter limiter = new RateLimiter(now::get);
        var limit = new GatewayProperties.Limit(2, Duration.ofSeconds(60));
        assertEquals(0, limiter.acquire("a", limit, 1));
        assertEquals(0, limiter.acquire("a", limit, 1));
        assertEquals(30, limiter.acquire("a", limit, 1));
        assertEquals(0, limiter.acquire("b", limit, 1));
        now.addAndGet(Duration.ofSeconds(30).toNanos());
        assertEquals(0, limiter.acquire("a", limit, 1));
        assertTrue(limiter.acquire("a", limit, 1) > 0);
    }
}
