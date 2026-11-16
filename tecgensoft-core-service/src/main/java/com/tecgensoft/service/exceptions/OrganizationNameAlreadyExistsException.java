package com.tecgensoft.service.exceptions;

import com.tecgensoft.common.exceptions.ConflictException;

public class OrganizationNameAlreadyExistsException extends ConflictException {
    public OrganizationNameAlreadyExistsException(Object... params) {
        super("organizationNameExists", params);
    }
}
