package com.tecgensoft.service.exceptions;

import com.tecgensoft.common.exceptions.BadRequestException;

public class InvalidSubscriptionPeriodException extends BadRequestException {
    public InvalidSubscriptionPeriodException(Object... params) {
        super("invalidSubscriptionPeriod", params);
    }
}
