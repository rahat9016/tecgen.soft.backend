package com.tecgensoft.http.dtos.requests;

import jakarta.validation.constraints.NotNull;

public record UpdatePackageAvailabilityRequest(
        @NotNull Boolean active,
        @NotNull Boolean availableForSale
) { }
