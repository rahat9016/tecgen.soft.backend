package com.tecgensoft.http.controllers;

import com.tecgensoft.http.dtos.requests.CreateOrganizationRequest;
import com.tecgensoft.http.dtos.requests.UpdateOrganizationRequest;
import com.tecgensoft.http.dtos.responses.OrganizationResponse;
import com.tecgensoft.http.dtos.responses.CreatedOrganizationResponse;
import com.tecgensoft.http.dtos.responses.SuccessResponse;
import com.tecgensoft.service.OrganizationService;
import com.tecgensoft.service.entities.Organization;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'ORGANIZATION_CREATE')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Create a new organization.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Organization created."),
                    @ApiResponse(responseCode = "400", description = "Invalid input."),
                    @ApiResponse(responseCode = "401", description = "Unauthenticated.")
            }
    )
    public ResponseEntity<SuccessResponse<CreatedOrganizationResponse>> createOrganization(
            @Valid @ModelAttribute CreateOrganizationRequest request
    ) {
        // 1. CONVERT REQUEST TO ENTITY
        Organization organization = request.toEntity();

        // 2. CREATE ORGANIZATION + ADMIN
        var createdOrganization = organizationService.createOrganization(
                organization,
                request.getLogo()
        );

        // 3. PREPARE RESPONSE
        CreatedOrganizationResponse response = new CreatedOrganizationResponse(createdOrganization);

        // 4. RETURN CREATED
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SuccessResponse.of("Organization created successfully", response));
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'ORGANIZATION_LIST')")
    public ResponseEntity<SuccessResponse<List<OrganizationResponse>>> getAllOrganizations() {
        List<OrganizationResponse> organizations = organizationService.getAllOrganizations().stream()
                .map(OrganizationResponse::new)
                .toList();
        return ResponseEntity.ok(SuccessResponse.of("Organizations retrieved successfully", organizations));
    }

    @GetMapping(ID)
    @PreAuthorize("@permissionAuthorizationService.hasOrganizationPermission(authentication, #organizationId, 'ORGANIZATION_READ')")
    public ResponseEntity<SuccessResponse<OrganizationResponse>> getOrganizationById(
            @PathVariable("id") UUID organizationId
    ) {
        Organization organization = organizationService.getOrganizationById(organizationId);
        return ResponseEntity.ok(
                SuccessResponse.of("Organization retrieved successfully", new OrganizationResponse(organization))
        );
    }

    @PatchMapping(ID)
    @PreAuthorize("@permissionAuthorizationService.hasOrganizationPermission(authentication, #organizationId, 'ORGANIZATION_UPDATE')")
    public ResponseEntity<SuccessResponse<OrganizationResponse>> updateOrganization(
            @PathVariable("id") UUID organizationId,
            @Valid @ModelAttribute UpdateOrganizationRequest request
    ) {
        Organization organization = organizationService.updateOrganization(
                organizationId,
                request.toEntity(),
                request.logo()
        );
        return ResponseEntity.ok(
                SuccessResponse.of("Organization updated successfully", new OrganizationResponse(organization))
        );
    }
}
