package com.tecgensoft.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalServiceKeyValidator {
    private final byte[] configuredKey;

    public InternalServiceKeyValidator(@Value("${internal.service-key:}") String serviceKey) {
        this.configuredKey = serviceKey.getBytes(StandardCharsets.UTF_8);
    }

    public void requireValid(String suppliedKey) {
        if (configuredKey.length == 0 || suppliedKey == null
                || !MessageDigest.isEqual(
                        configuredKey,
                        suppliedKey.getBytes(StandardCharsets.UTF_8)
                )) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid service credentials");
        }
    }
}
