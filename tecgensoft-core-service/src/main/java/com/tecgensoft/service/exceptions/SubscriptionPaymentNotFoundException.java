package com.tecgensoft.service.exceptions;

import com.tecgensoft.common.exceptions.ResourceNotFoundException;

public class SubscriptionPaymentNotFoundException extends ResourceNotFoundException {
    public SubscriptionPaymentNotFoundException(Object... params) {
        super("subscriptionPaymentNotFound", params);
    }
}
