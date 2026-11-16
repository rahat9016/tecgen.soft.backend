package com.tecgensoft.service.entities;

public record CreatedBranchResult(
        Branch branch,
        BranchAdminCredentials admin
) {}
