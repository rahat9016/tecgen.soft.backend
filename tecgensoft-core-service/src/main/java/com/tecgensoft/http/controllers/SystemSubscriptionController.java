package com.tecgensoft.http.controllers;

import com.tecgensoft.http.dtos.requests.AssignSubscriptionRequest;
import com.tecgensoft.http.dtos.requests.UpdateSubscriptionStatusRequest;
import com.tecgensoft.http.dtos.responses.SubscriptionResponse;
import com.tecgensoft.http.dtos.requests.CreateSubscriptionPaymentRequest;
import com.tecgensoft.http.dtos.responses.SubscriptionPaymentResponse;
import com.tecgensoft.jpa.daos.SubscriptionPaymentDao;
import com.tecgensoft.http.dtos.responses.SuccessResponse;
import com.tecgensoft.service.SubscriptionService;
import com.tecgensoft.service.entities.Subscription;
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
import static com.tecgensoft.common.utils.Constants.ORGANIZATIONS;
import static com.tecgensoft.common.utils.Constants.SYSTEM;

@Tag(name = "Subscription Management", description = "Organization package subscriptions")
@RestController
@RequestMapping(API_BASE + SYSTEM + ORGANIZATIONS + "/{organizationId}/subscriptions")
@RequiredArgsConstructor
public class SystemSubscriptionController {
    private final SubscriptionService subscriptionService;
    private final com.tecgensoft.service.PermissionAuthorizationService authorization;

    @GetMapping
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'SUBSCRIPTION_VIEW')")
    public ResponseEntity<SuccessResponse<List<SubscriptionResponse>>> getSubscriptionHistory(
            @PathVariable UUID organizationId
    ) {
        List<SubscriptionResponse> history = subscriptionService.getHistory(organizationId).stream()
                .map(SubscriptionResponse::new)
                .toList();
        return ResponseEntity.ok(SuccessResponse.of("Subscriptions retrieved successfully", history));
    }

    @GetMapping("/{subscriptionId}")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'SUBSCRIPTION_VIEW')")
    public ResponseEntity<SuccessResponse<SubscriptionResponse>> getSubscription(
        @PathVariable UUID organizationId,@PathVariable UUID subscriptionId) {
        return ResponseEntity.ok(SuccessResponse.of("Subscription retrieved",
            new SubscriptionResponse(subscriptionService.get(organizationId,subscriptionId))));
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'SUBSCRIPTION_CREATE') or @permissionAuthorizationService.hasPermission(authentication, 'SUBSCRIPTION_ASSIGN')")
    public ResponseEntity<SuccessResponse<SubscriptionResponse>> assignSubscription(
            @PathVariable UUID organizationId,
            @Valid @RequestBody AssignSubscriptionRequest request
    ) {
        Subscription created = subscriptionService.assign(organizationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                SuccessResponse.of("Subscription assigned successfully", new SubscriptionResponse(created))
        );
    }

    @PatchMapping("/{subscriptionId}/status")
    public ResponseEntity<SuccessResponse<SubscriptionResponse>> updateStatus(
            @PathVariable UUID organizationId,
            @PathVariable UUID subscriptionId,
            @Valid @RequestBody UpdateSubscriptionStatusRequest request
    ) {
        String permission=switch(request.status()) {
            case ACTIVE -> "SUBSCRIPTION_ACTIVATE";
            case SUSPENDED -> "SUBSCRIPTION_SUSPEND";
            case CANCELLED -> "SUBSCRIPTION_CANCEL";
            default -> "SUBSCRIPTION_UPDATE";
        };
        if(!authorization.hasPermission(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication(),permission))
            throw new org.springframework.security.access.AccessDeniedException("Subscription permission required");
        Subscription updated = subscriptionService.updateStatus(organizationId, subscriptionId, request);
        return ResponseEntity.ok(
                SuccessResponse.of("Subscription status updated successfully", new SubscriptionResponse(updated))
        );
    }

    @PostMapping("/{subscriptionId}/renewal")
    public ResponseEntity<SuccessResponse<SubscriptionResponse>> renew(@PathVariable UUID organizationId,
        @PathVariable UUID subscriptionId) {
        var auth=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if(!authorization.hasPermission(auth,"SUBSCRIPTION_RENEW")) throw new org.springframework.security.access.AccessDeniedException("Permission required");
        return ResponseEntity.ok(SuccessResponse.of("Subscription renewed",new SubscriptionResponse(subscriptionService.renew(organizationId,subscriptionId))));
    }

    public record TrialExtension(@jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(365) int days) {}
    @PostMapping("/{subscriptionId}/trial-extension")
    public ResponseEntity<SuccessResponse<SubscriptionResponse>> extendTrial(@PathVariable UUID organizationId,
        @PathVariable UUID subscriptionId,@Valid @RequestBody TrialExtension request) {
        var auth=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if(!authorization.hasPermission(auth,"SUBSCRIPTION_EXTEND_TRIAL")) throw new org.springframework.security.access.AccessDeniedException("Permission required");
        return ResponseEntity.ok(SuccessResponse.of("Trial extended",new SubscriptionResponse(subscriptionService.extendTrial(organizationId,subscriptionId,request.days()))));
    }

    @GetMapping("/{subscriptionId}/payments")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'SUBSCRIPTION_PAYMENT_VIEW')")
    public List<SubscriptionPaymentResponse> payments(@PathVariable UUID organizationId,@PathVariable UUID subscriptionId){
        return subscriptionService.payments(organizationId,subscriptionId).stream().map(SubscriptionPaymentResponse::new).toList();
    }

    @PostMapping("/{subscriptionId}/payments")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'SUBSCRIPTION_PAYMENT_CREATE')")
    public ResponseEntity<SuccessResponse<SubscriptionPaymentResponse>> createPayment(@PathVariable UUID organizationId,
        @PathVariable UUID subscriptionId,@Valid @RequestBody CreateSubscriptionPaymentRequest request){
        var payment=new SubscriptionPaymentDao();payment.setAmount(request.amount());payment.setCurrency(request.currency());
        payment.setPaymentMethod(request.paymentMethod());payment.setPaymentStatus(request.paymentStatus());
        payment.setTransactionId(request.transactionId());payment.setPaymentDate(request.paymentDate());
        payment.setPeriodStart(request.periodStart());payment.setPeriodEnd(request.periodEnd());
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.of("Payment recorded",
            new SubscriptionPaymentResponse(subscriptionService.createPayment(organizationId,subscriptionId,payment))));
    }

    @PatchMapping("/{subscriptionId}/payments/{paymentId}/refund")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'SUBSCRIPTION_PAYMENT_REFUND')")
    public SubscriptionPaymentResponse refund(@PathVariable UUID organizationId,@PathVariable UUID subscriptionId,@PathVariable UUID paymentId){
        return new SubscriptionPaymentResponse(subscriptionService.refundPayment(organizationId,subscriptionId,paymentId));
    }
}
