
package com.tecgensoft.http.dtos.responses;

public record ActivateOrganizationAdminResponse(
        String email,
        boolean active
) {
}
