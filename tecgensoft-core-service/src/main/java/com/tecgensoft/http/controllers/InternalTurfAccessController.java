package com.tecgensoft.http.controllers;

import com.tecgensoft.service.InternalServiceKeyValidator;
import com.tecgensoft.service.OrganizationDomainService;
import com.tecgensoft.service.PermissionAuthorizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternalTurfAccessController {
    private final OrganizationDomainService domains;
    private final InternalServiceKeyValidator serviceKey;
    private final PermissionAuthorizationService permissions;

    public record TenantRequest(@NotBlank String hostname) {}
    public record TenantResponse(UUID organizationId, String product, boolean active) {}
    public record AccessRequest(@NotNull UUID organizationId, @NotBlank String product,
                               @NotBlank String permissionCode) {}
    public record AccessResponse(boolean allowed, String reasonCode, UUID userId) {}

    @PostMapping("/tenants/resolve")
    public TenantResponse resolve(@RequestHeader(value="X-Service-Key", required=false) String key,
                                  @Valid @RequestBody TenantRequest request) {
        serviceKey.requireValid(key);
        return domains.findVerified(request.hostname())
                .map(tenant -> new TenantResponse(tenant.organizationId(), tenant.product(), tenant.active()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hostname is not registered"));
    }

    @PostMapping("/access/check")
    public AccessResponse check(@RequestHeader(value="X-Service-Key", required=false) String key,
                                @RequestHeader(value="Authorization", required=false) String authorization,
                                @Valid @RequestBody AccessRequest request,
                                Authentication authentication) {
        serviceKey.requireValid(key);
        UUID userId = null;
        try { if (authentication != null) userId = UUID.fromString(authentication.getName()); }
        catch (IllegalArgumentException ignored) { }
        if (userId == null || authorization == null || !"TURF".equals(request.product()))
            return new AccessResponse(false, "AUTHENTICATION_REQUIRED", userId);
        boolean allowed = permissions.hasOrganizationPermission(authentication,
                request.organizationId(), request.permissionCode());
        return new AccessResponse(allowed, allowed ? "ALLOWED" : "PERMISSION_DENIED", userId);
    }
}
