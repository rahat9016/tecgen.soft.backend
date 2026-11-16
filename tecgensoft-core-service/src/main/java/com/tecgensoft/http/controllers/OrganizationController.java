package com.tecgensoft.http.controllers;

import com.tecgensoft.http.dtos.requests.CreateOrganizationRequest;
import com.tecgensoft.http.dtos.requests.UpdateOrganizationRequest;
import com.tecgensoft.http.dtos.responses.OrganizationResponse;
import com.tecgensoft.http.dtos.responses.SuccessResponse;
import com.tecgensoft.service.OrganizationService;
import com.tecgensoft.service.entities.Organization;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.tecgensoft.common.utils.Constants.*;

@Tag(name = "Organization Management", description = "Platform organization management")
@RestController
@RequestMapping(API_BASE + ORGANIZATIONS)
@RequiredArgsConstructor
public class OrganizationController {
    private final OrganizationService organizationService;

    @PostMapping
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'BRANCH_CREATE')")
    public ResponseEntity<SuccessResponse<OrganizationResponse>> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request
    ) {
        Organization organization = organizationService.createOrganization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                SuccessResponse.of("Organization created successfully", new OrganizationResponse(organization))
        );
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'BRANCH_LIST')")
    public ResponseEntity<SuccessResponse<List<OrganizationResponse>>> getAllOrganizations() {
        List<OrganizationResponse> organizations = organizationService.getAllOrganizations().stream()
                .map(OrganizationResponse::new)
                .toList();
        return ResponseEntity.ok(SuccessResponse.of("Organizations retrieved successfully", organizations));
    }

    @GetMapping(ID)
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'BRANCH_LIST')")
    public ResponseEntity<SuccessResponse<OrganizationResponse>> getOrganizationById(
            @PathVariable("id") UUID organizationId
    ) {
        Organization organization = organizationService.getOrganizationById(organizationId);
        return ResponseEntity.ok(
                SuccessResponse.of("Organization retrieved successfully", new OrganizationResponse(organization))
        );
    }

    @PatchMapping(ID)
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'BRANCH_UPDATE')")
    public ResponseEntity<SuccessResponse<OrganizationResponse>> updateOrganization(
            @PathVariable("id") UUID organizationId,
            @Valid @RequestBody UpdateOrganizationRequest request
    ) {
        Organization organization = organizationService.updateOrganization(organizationId, request);
        return ResponseEntity.ok(
                SuccessResponse.of("Organization updated successfully", new OrganizationResponse(organization))
        );
    }
}
