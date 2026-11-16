package com.tecgensoft.service.exceptions;

import com.tecgensoft.common.exceptions.BusinessException;

public class InvalidRolePermissionAssignmentException
        extends BusinessException {

    public InvalidRolePermissionAssignmentException(
            Object... params
    ) {
        super("invalidRolePermissionAssignment", params);
    }
}
