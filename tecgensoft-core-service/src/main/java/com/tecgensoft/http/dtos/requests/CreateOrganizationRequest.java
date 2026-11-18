package com.tecgensoft.http.dtos.requests;

import com.tecgensoft.service.entities.Organization;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Setter
@Getter
@NoArgsConstructor
public class CreateOrganizationRequest {
    @NotBlank(message = "Organization name is required")
    @Size(max = 255,
            message = "Organization name cannot exceed 255 characters")
    @Schema(description = "Organization name",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Acme Inc.")
    private String name;

    @Schema(description = "Organization address",
            example = "123 Main Street, Dhaka")
    private String address;

    @Size(max = 30,
            message = "Phone number cannot exceed 30 characters")
    @Schema(description = "Organization phone number",
            example = "+8801712345678")
    private String phone;

    @Email(message = "Organization email must be valid")
    @Size(max = 254,
            message = "Organization email cannot exceed 254 characters")
    @Schema(description = "Organization email address",
            example = "contact@acme.com")
    private String email;

    @Schema(description = "Organization logo",
            type = "string",
            format = "binary")
    private MultipartFile logo;

    public Organization toEntity() {
        return Organization.builder()
                .name(name.trim())
                .address(address)
                .phone(phone)
                .email(email)
                .isActive(true)
                .build();
    }
}
