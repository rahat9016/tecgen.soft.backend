package com.tecgensoft.http.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @Size(max = 255, message = "Organization name cannot exceed 255 characters")
        String name,
        String address,
        @Size(max = 30, message = "Phone number cannot exceed 30 characters")
        String phone,
        @Email(message = "Organization email must be valid")
        @Size(max = 254, message = "Organization email cannot exceed 254 characters")
        String email,
        Boolean isActive
) { }
