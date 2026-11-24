package com.tecgensoft.http.dtos.requests;

import jakarta.validation.constraints.Size;
import com.tecgensoft.service.entities.Organization;
import org.springframework.web.multipart.MultipartFile;

public record UpdateOrganizationRequest(
        @Size(max = 255, message = "Organization name cannot exceed 255 characters")
        String name,
        String address,
        @Size(max = 30, message = "Phone number cannot exceed 30 characters")
        String phone,
        Boolean isActive,
        MultipartFile logo
) {
    public Organization toEntity() {
        return Organization.builder()
                .name(name)
                .address(address)
                .phone(phone)
                .isActive(isActive)
                .build();
    }
}
