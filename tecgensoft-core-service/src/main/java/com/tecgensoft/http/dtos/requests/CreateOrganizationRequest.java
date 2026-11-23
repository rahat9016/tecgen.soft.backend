package com.tecgensoft.http.dtos.requests;

import com.tecgensoft.service.entities.Organization;
import com.tecgensoft.service.entities.SubscriptionSetup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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

    @NotBlank(message = "Admin email is required")
    @Email(message = "Admin email must be valid")
    @Size(max = 254, message = "Admin email cannot exceed 254 characters")
    @Schema(description = "Organization admin email",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "admin@acme.com")
    private String adminEmail;

    @NotBlank(message = "Admin password is required")
    @Size(min = 8, max = 72, message = "Admin password must be 8 to 72 characters")
    @Schema(description = "Organization admin password",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Admin@12345")
    private String adminPassword;

    @NotNull(message = "Package is required")
    @Schema(description = "Package to subscribe",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID packageId;

    @Schema(description = "Start as free trial", example = "true")
    private boolean isFreeTrial = false;

    @NotNull(message = "Start date is required")
    @Schema(description = "Subscription start", example = "2026-11-23T00:00:00Z")
    private Instant startsAt;

    @NotNull(message = "End date is required")
    @Schema(description = "Subscription end", example = "2027-01-23T00:00:00Z")
    private Instant endsAt;

    @NotNull(message = "Grace days is required")
    @Min(value = 0, message = "Grace days cannot be negative")
    @Schema(description = "Payment grace days after due (per organization)", example = "7")
    private Integer graceDays;

    @Schema(description = "Amount paid now for non-trial. Null or 0 becomes DUE", example = "0")
    private BigDecimal amountPaid;

    public Organization toEntity() {
        return Organization.builder()
                .name(name.trim())
                .address(address)
                .phone(phone)
                .email(email)
                .isActive(true)
                .build();
    }

    public SubscriptionSetup toSubscriptionSetup() {
        return SubscriptionSetup.builder()
                .packageId(packageId)
                .isFreeTrial(isFreeTrial)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .amountPaid(amountPaid)
                .graceDays(graceDays)
                .build();
    }
}
