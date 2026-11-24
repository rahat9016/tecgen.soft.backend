package com.tecgensoft.http.dtos.responses;

import java.util.List;

public record OrganizationDetailResponse(
        OrganizationResponse organization,
        List<String> domains,
        List<SubscriptionResponse> subscriptions
) { }
