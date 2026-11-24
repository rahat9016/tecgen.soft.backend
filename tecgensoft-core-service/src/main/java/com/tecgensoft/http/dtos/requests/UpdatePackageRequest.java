package com.tecgensoft.http.dtos.requests;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import com.tecgensoft.service.enums.BillingPeriod;

public record UpdatePackageRequest(@NotBlank String displayName,String description,BillingPeriod billingPeriod,
    @NotBlank @Pattern(regexp="[A-Z]{3}") String currency,
    @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal price,
    Boolean active,
    Boolean availableForSale) {}
