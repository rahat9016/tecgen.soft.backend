package com.tecgensoft.service.exceptions;

import com.tecgensoft.common.exceptions.ResourceNotFoundException;

public class OrganizationNotFoundException extends ResourceNotFoundException {
    public OrganizationNotFoundException(Object... params) {
        super("organizationNotFound", params);
    }
}
