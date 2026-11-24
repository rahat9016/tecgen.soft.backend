package com.tecgensoft.http.dtos.responses;

import java.util.List;
import java.util.UUID;

public record PublicTenantResponse(
        UUID organizationId,
        String name,
        String logoUrl,
        String product,
        List<String> features
) {
}
