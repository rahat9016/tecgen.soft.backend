package com.tecgensoft.http.dtos.requests;

import com.tecgensoft.service.entities.SubscriptionPlan;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Setter
@Getter
@NoArgsConstructor
public class CreatePackageRequest {

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9_-]{2,60}")
    private String code;

    @NotBlank
    @Size(max = 120)
    private String displayName;

    @NotNull
    private ProductCode product;

    private BillingPeriod billingPeriod;

    @Size(max = 2000)
    private String description;

    @NotBlank
    @Pattern(regexp = "[A-Z]{3}")
    private String currency;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal price;

    @NotEmpty
    private Set<@NotBlank String> features;

    @NotNull
    private Map<@NotBlank String, @NotNull @Min(-1) Integer> limits;

    public SubscriptionPlan toEntity() {
        return SubscriptionPlan.builder()
                .code(code.trim().toUpperCase(Locale.ROOT))
                .displayName(displayName.trim())
                .product(product)
                .billingPeriod(billingPeriod == null ? BillingPeriod.MONTHLY : billingPeriod)
                .description(description == null ? null : description.trim())
                .currency(currency.trim().toUpperCase(Locale.ROOT))
                .price(price)
                .features(features.stream()
                        .map(value -> value.trim().toUpperCase(Locale.ROOT))
                        .collect(Collectors.toUnmodifiableSet()))
                .limits(limits.entrySet().stream()
                        .collect(Collectors.toUnmodifiableMap(
                                entry -> entry.getKey().trim().toUpperCase(Locale.ROOT),
                                Map.Entry::getValue)))
                .active(true)
                .availableForSale(true)
                .build();
    }
}
