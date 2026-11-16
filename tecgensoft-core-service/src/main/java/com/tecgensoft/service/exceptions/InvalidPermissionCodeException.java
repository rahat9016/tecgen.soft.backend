package com.tecgensoft.service.exceptions;

import com.tecgensoft.common.exceptions.BusinessException;

public class InvalidPermissionCodeException extends BusinessException {
    public InvalidPermissionCodeException(Object... params) {
        super("invalidPermissionCode", params);
    }
}