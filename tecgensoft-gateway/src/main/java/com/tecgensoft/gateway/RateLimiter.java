package com.tecgensoft.gateway;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Token buckets for a single gateway instance. Move them to Redis before running more than one. */
@Component
public class RateLimiter {
    private final Ticker ticker;
    private final Cache<String, Bucket> buckets;

    @Autowired
    public RateLimiter() {
        this(Ticker.systemTicker());
    }

    RateLimiter(Ticker ticker) {
        this.ticker = ticker;
        this.buckets = Caffeine.newBuilder().ticker(ticker).maximumSize(200_000)
                .expireAfterAccess(Duration.ofMinutes(15)).build();
    }

    /** Takes one token. Returns zero when allowed, otherwise the seconds to wait. */
    public long acquire(String key, GatewayProperties.Limit limit, int multiplier) {
        double capacity = (double) limit.requests() * multiplier;
        return buckets.get(key, ignored -> new Bucket(capacity, ticker.read()))
                .take(capacity, capacity / limit.per().toNanos(), ticker.read());
    }

    private static final class Bucket {
        private double tokens;
        private long updated;

        private Bucket(double tokens, long updated) {
            this.tokens = tokens;
            this.updated = updated;
        }

        private synchronized long take(double capacity, double perNano, long now) {
            tokens = Math.min(capacity, tokens + Math.max(0, now - updated) * perNano);
            updated = Math.max(updated, now);
            if (tokens >= 1) {
                tokens--;
                return 0;
            }
            return Math.max(1, (long) Math.ceil((1 - tokens) / perNano / 1_000_000_000d));
        }
    }
}
