package com.tecgensoft.http.controllers;

import com.tecgensoft.http.dtos.responses.EntitlementsResponse;
import com.tecgensoft.service.InternalServiceKeyValidator;
import com.tecgensoft.service.SubscriptionService;
import com.tecgensoft.service.enums.ProductCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/organizations/{organizationId}/entitlements")
@RequiredArgsConstructor
public class InternalEntitlementsController {
    private final InternalServiceKeyValidator serviceKeyValidator;
    private final SubscriptionService subscriptionService;

    @GetMapping
    public EntitlementsResponse getEntitlements(
            @PathVariable UUID organizationId,
            @RequestParam ProductCode product,
            @RequestHeader(name = "X-Service-Key", required = false) String serviceKey
    ) {
        serviceKeyValidator.requireValid(serviceKey);
        SubscriptionService.EntitlementSnapshot result =
                subscriptionService.getEntitlements(organizationId, product);
        return new EntitlementsResponse(
                result.organizationId(), result.packageId(), result.packageCode(), result.version(),
                result.product(), result.product().name(), result.subscriptionStatus(), result.active(), result.active(), result.isTrial(),
                result.trialDaysRemaining(), result.features(), result.limits()
        );
    }
}
