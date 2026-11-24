package com.tecgensoft.http.dtos.responses;

import java.util.List;

public record OrganizationSummaryResponse(
        OrganizationResponse organization,
        List<String> domains,
        String currentSubscriptionStatus
) { }
