package com.tecgensoft.service.exceptions;

import com.tecgensoft.common.exceptions.BadRequestException;

public class UnsupportedPaymentMethodException extends BadRequestException {
    public UnsupportedPaymentMethodException(Object... params) {
        super("unsupportedPaymentMethod", params);
    }
}
