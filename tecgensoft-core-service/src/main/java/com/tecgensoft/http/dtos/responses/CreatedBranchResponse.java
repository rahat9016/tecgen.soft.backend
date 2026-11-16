package com.tecgensoft.http.dtos.responses;

import com.tecgensoft.service.entities.CreatedBranchResult;

public record CreatedBranchResponse(
        BranchResponse branch,
        AdminResponse admin
) {

    public CreatedBranchResponse(
            CreatedBranchResult result,
            String logoUrl
    ) {
        this(
                new BranchResponse(
                        result.branch(),
                        logoUrl
                ),
                new AdminResponse(result.admin())
        );
    }
}