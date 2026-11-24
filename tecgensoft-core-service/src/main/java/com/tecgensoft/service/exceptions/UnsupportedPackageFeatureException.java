package com.tecgensoft.service.exceptions;

import com.tecgensoft.common.exceptions.BadRequestException;

public class UnsupportedPackageFeatureException extends BadRequestException {
    public UnsupportedPackageFeatureException(Object... params) {
        super("unsupportedPackageFeature", params);
    }
}
