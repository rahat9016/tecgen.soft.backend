package com.tecgensoft.http.dtos.requests;

import com.tecgensoft.service.enums.ProductCode;
import com.tecgensoft.service.enums.BillingPeriod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

public record CreatePackageRequest(
        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9_-]{2,60}")
        String code,

        @NotBlank
        @Size(max = 120)
        String displayName,

        @NotNull
        ProductCode product,

        BillingPeriod billingPeriod,

        @Size(max = 2000)
        String description,

        @NotBlank
        @Pattern(regexp = "[A-Z]{3}")
        String currency,

        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @NotEmpty
        Set<@NotBlank String> features,

        @NotNull
        Map<@NotBlank String, @NotNull @Min(-1) Integer> limits
) { }
