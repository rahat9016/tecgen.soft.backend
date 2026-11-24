package com.tecgensoft.http.controllers;

import com.tecgensoft.common.exceptions.ErrorResponse;
import com.tecgensoft.http.dtos.responses.PublicTenantResponse;
import com.tecgensoft.http.dtos.responses.SuccessResponse;
import com.tecgensoft.service.OrganizationDomainService;
import com.tecgensoft.service.PublicTenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tecgensoft.common.utils.Constants.*;

@Tag(name = "Public Tenant", description = "Site information for the organization behind a hostname")
@RestController
@RequestMapping(API_BASE + PUBLIC + TENANT)
@RequiredArgsConstructor
public class PublicTenantController {
    public static final String TENANT_HOST_HEADER = "X-Tenant-Host";

    private final OrganizationDomainService organizationDomainService;
    private final PublicTenantService publicTenantService;

    @GetMapping
    @Operation(
            summary = "Get the organization of a site",
            description = "The gateway sets X-Tenant-Host from the verified browser Origin. The header only selects an organization; it never grants access."
    )
    public ResponseEntity<?> tenant(
            @RequestHeader(name = TENANT_HOST_HEADER, required = false) String hostname
    ) {
        if (hostname == null || hostname.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "TENANT_REQUIRED", "Tenant host is required");
        }
        OrganizationDomainService.DomainTenant tenant =
                organizationDomainService.findVerified(hostname).orElse(null);
        if (tenant == null) {
            return error(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "Organization not found");
        }
        PublicTenantResponse response = publicTenantService.describe(tenant).orElse(null);
        if (response == null) {
            return error(HttpStatus.FORBIDDEN, "TENANT_INACTIVE", "Organization is not active");
        }
        return ResponseEntity.ok(SuccessResponse.of("Success", response));
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), code, message));
    }
}
