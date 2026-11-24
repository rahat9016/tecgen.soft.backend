package com.tecgensoft.http.controllers;

import com.tecgensoft.http.dtos.requests.CreatePackageRequest;
import com.tecgensoft.http.dtos.requests.UpdatePackageRequest;
import com.tecgensoft.http.dtos.responses.PackageResponse;
import com.tecgensoft.http.dtos.responses.SuccessResponse;
import com.tecgensoft.service.PackageService;
import com.tecgensoft.service.entities.SubscriptionPlan;
import com.tecgensoft.service.enums.ProductCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

@Tag(name = "Package Management", description = "Platform software package catalog")
@RestController
@RequestMapping(API_BASE + SYSTEM + PACKAGES)
@RequiredArgsConstructor
public class SystemPackageController {
    private final PackageService packageService;

    @GetMapping
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'PACKAGE_VIEW')")
    @Operation(
            summary = "List packages.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Packages retrieved."),
                    @ApiResponse(responseCode = "401", description = "Unauthenticated.")
            }
    )
    public ResponseEntity<SuccessResponse<List<PackageResponse>>> getPackages(
            @RequestParam(required = false) ProductCode product
    ) {
        List<PackageResponse> packages = packageService.getAll(product).stream()
                .map(PackageResponse::new)
                .toList();
        return ResponseEntity.ok(SuccessResponse.of("Packages retrieved successfully", packages));
    }

    @GetMapping(PACKAGE_ID)
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'PACKAGE_VIEW')")
    @Operation(
            summary = "Get package by id.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Package retrieved."),
                    @ApiResponse(responseCode = "404", description = "Package not found.")
            }
    )
    public ResponseEntity<SuccessResponse<PackageResponse>> getPackage(@PathVariable UUID packageId) {
        return ResponseEntity.ok(SuccessResponse.of("Package retrieved",new PackageResponse(packageService.get(packageId))));
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'PACKAGE_CREATE')")
    @Operation(
            summary = "Create a package.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Package created."),
                    @ApiResponse(responseCode = "400", description = "Invalid input.")
            }
    )
    public ResponseEntity<SuccessResponse<PackageResponse>> createPackage(
            @Valid @RequestBody CreatePackageRequest request
    ) {
        SubscriptionPlan created = packageService.create(request.toEntity());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                SuccessResponse.of("Package created successfully", new PackageResponse(created))
        );
    }

    @PatchMapping(PACKAGE_ID)
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'PACKAGE_UPDATE')")
    @Operation(
            summary = "Update a package.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Package updated."),
                    @ApiResponse(responseCode = "404", description = "Package not found.")
            }
    )
    public ResponseEntity<SuccessResponse<PackageResponse>> update(@PathVariable UUID packageId,
        @Valid @RequestBody UpdatePackageRequest request) {
        return ResponseEntity.ok(SuccessResponse.of("Package updated",new PackageResponse(packageService.update(packageId,request))));
    }
}
