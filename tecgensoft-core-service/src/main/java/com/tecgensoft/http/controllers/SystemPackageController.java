package com.tecgensoft.http.controllers;

import com.tecgensoft.http.dtos.requests.CreatePackageRequest;
import com.tecgensoft.http.dtos.requests.UpdatePackageAvailabilityRequest;
import com.tecgensoft.http.dtos.requests.UpdatePackageRequest;
import com.tecgensoft.http.dtos.responses.PackageResponse;
import com.tecgensoft.http.dtos.responses.SuccessResponse;
import com.tecgensoft.service.PackageService;
import com.tecgensoft.service.entities.SoftwarePackage;
import com.tecgensoft.service.enums.ProductCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.tecgensoft.common.utils.Constants.API_BASE;
import static com.tecgensoft.common.utils.Constants.SYSTEM;

@Tag(name = "Package Management", description = "Platform software package catalog")
@RestController
@RequestMapping(API_BASE + SYSTEM + "/packages")
@RequiredArgsConstructor
public class SystemPackageController {
    private final PackageService packageService;
    private final com.tecgensoft.service.PermissionAuthorizationService authorization;

    @GetMapping
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'PACKAGE_VIEW')")
    public ResponseEntity<SuccessResponse<List<PackageResponse>>> getPackages(
            @RequestParam(required = false) ProductCode product
    ) {
        List<PackageResponse> packages = packageService.getAll(product).stream()
                .map(PackageResponse::new)
                .toList();
        return ResponseEntity.ok(SuccessResponse.of("Packages retrieved successfully", packages));
    }

    @GetMapping("/{packageId}")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'PACKAGE_VIEW')")
    public ResponseEntity<SuccessResponse<PackageResponse>> getPackage(@PathVariable UUID packageId) {
        return ResponseEntity.ok(SuccessResponse.of("Package retrieved",new PackageResponse(packageService.get(packageId))));
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'PACKAGE_CREATE')")
    public ResponseEntity<SuccessResponse<PackageResponse>> createPackage(
            @Valid @RequestBody CreatePackageRequest request
    ) {
        SoftwarePackage created = packageService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                SuccessResponse.of("Package created successfully", new PackageResponse(created))
        );
    }

    @PatchMapping("/{packageId}/availability")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'PACKAGE_UPDATE')")
    public ResponseEntity<SuccessResponse<PackageResponse>> updateAvailability(
            @PathVariable UUID packageId,
            @Valid @RequestBody UpdatePackageAvailabilityRequest request
    ) {
        SoftwarePackage updated = packageService.updateAvailability(packageId, request);
        return ResponseEntity.ok(
                SuccessResponse.of("Package availability updated successfully", new PackageResponse(updated))
        );
    }

    @PatchMapping("/{packageId}/activation")
    public ResponseEntity<SuccessResponse<PackageResponse>> setActive(@PathVariable UUID packageId,
        @Valid @RequestBody PackageActivation request,
        org.springframework.security.core.Authentication authentication) {
        String permission=request.active()?"PACKAGE_ACTIVATE":"PACKAGE_DEACTIVATE";
        if(!authorization.hasPermission(authentication,permission))
            throw new org.springframework.security.access.AccessDeniedException("Package permission required");
        var current=packageService.get(packageId);
        var changed=packageService.updateAvailability(packageId,new UpdatePackageAvailabilityRequest(request.active(),current.availableForSale()));
        return ResponseEntity.ok(SuccessResponse.of("Package activation updated",new PackageResponse(changed)));
    }
    public record PackageActivation(@jakarta.validation.constraints.NotNull Boolean active) {}

    @PatchMapping("/{packageId}")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'PACKAGE_UPDATE')")
    public ResponseEntity<SuccessResponse<PackageResponse>> update(@PathVariable UUID packageId,
        @Valid @RequestBody UpdatePackageRequest request) {
        return ResponseEntity.ok(SuccessResponse.of("Package updated",new PackageResponse(packageService.update(packageId,request))));
    }
}
