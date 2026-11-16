package com.tecgensoft.service.exceptions;

import com.tecgensoft.common.exceptions.ConflictException;

public class RoleAlreadyExistsException extends ConflictException {
    public RoleAlreadyExistsException(Object ...params) {
        super("roleAlreadyExists", params);
    }
}
