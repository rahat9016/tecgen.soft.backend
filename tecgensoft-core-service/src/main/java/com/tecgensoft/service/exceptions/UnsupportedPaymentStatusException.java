package com.tecgensoft.service.exceptions;

import com.tecgensoft.common.exceptions.BadRequestException;

public class UnsupportedPaymentStatusException extends BadRequestException {
    public UnsupportedPaymentStatusException(Object... params) {
        super("unsupportedPaymentStatus", params);
    }
}
