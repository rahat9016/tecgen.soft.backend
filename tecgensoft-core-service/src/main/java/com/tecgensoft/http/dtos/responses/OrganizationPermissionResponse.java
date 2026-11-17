
package com.tecgensoft.http.dtos.responses;

import java.util.List;
import java.util.UUID;

public record OrganizationPermissionResponse(
        UUID organizationId,
        String organizationCode,
        String organizationName,
        List<String> roles,
        List<ModulePermissionTreeResponse> permissions
) {
}
