package com.tecgensoft.service.exceptions;

import com.tecgensoft.common.exceptions.ResourceNotFoundException;

public class PackageNotFoundException extends ResourceNotFoundException {
    public PackageNotFoundException(Object... params) {
        super("availablePackageNotFound", params);
    }
}
