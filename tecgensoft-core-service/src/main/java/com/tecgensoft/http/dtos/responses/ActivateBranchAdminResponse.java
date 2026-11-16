
package com.tecgensoft.http.dtos.responses;

public record ActivateBranchAdminResponse(
        String email,
        boolean active
) {
}
