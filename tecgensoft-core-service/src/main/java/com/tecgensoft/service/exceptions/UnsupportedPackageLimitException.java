package com.tecgensoft.service.exceptions;

import com.tecgensoft.common.exceptions.BadRequestException;

public class UnsupportedPackageLimitException extends BadRequestException {
    public UnsupportedPackageLimitException(Object... params) {
        super("unsupportedPackageLimit", params);
    }
}
