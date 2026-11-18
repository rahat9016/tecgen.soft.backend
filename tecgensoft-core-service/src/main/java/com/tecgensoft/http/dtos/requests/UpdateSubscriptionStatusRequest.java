package com.tecgensoft.http.dtos.requests;

import com.tecgensoft.service.enums.SubscriptionStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSubscriptionStatusRequest(
        @NotNull SubscriptionStatus status
) { }
