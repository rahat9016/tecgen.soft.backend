
package com.tecgensoft.http.controllers;

import com.tecgensoft.http.dtos.requests.ActivateOrganizationAdminRequest;
import com.tecgensoft.http.dtos.responses.ActivateOrganizationAdminResponse;
import com.tecgensoft.http.dtos.responses.SuccessResponse;

import com.tecgensoft.service.AuthService;
import com.tecgensoft.service.entities.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.tecgensoft.common.utils.Constants.API_BASE;
import static com.tecgensoft.common.utils.Constants.AUTH;

@Tag(
        name = "Authentication",
        description = "Development-only Organization Admin activation"
)
@RestController
@RequestMapping(API_BASE + AUTH)
@RequiredArgsConstructor
public class OrganizationAdminActivationController {

    private final AuthService authService;

    @PostMapping("/activate-organization-admin")
    @Operation(
            summary = "Activate Organization Admin",
            description = "Development-only password-based activation",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Organization Admin activated"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid new password"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Invalid activation credentials"
                    )
            }
    )
    public ResponseEntity<
            SuccessResponse<ActivateOrganizationAdminResponse>
            > activateOrganizationAdmin(
            @Valid @RequestBody
            ActivateOrganizationAdminRequest request
    ) {

        User activatedAdmin =
                authService.activateOrganizationAdmin(request);

        ActivateOrganizationAdminResponse response =
                new ActivateOrganizationAdminResponse(
                        activatedAdmin.email(),
                        activatedAdmin.isActive()
                );

        return ResponseEntity.ok(
                SuccessResponse.of(
                        "Organization Admin activated successfully",
                        response
                )
        );
    }
}
