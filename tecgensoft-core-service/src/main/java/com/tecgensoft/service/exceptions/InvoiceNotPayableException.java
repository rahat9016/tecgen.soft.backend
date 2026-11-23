package com.tecgensoft.service.exceptions;

import com.tecgensoft.common.exceptions.ConflictException;

public class InvoiceNotPayableException extends ConflictException {
    public InvoiceNotPayableException(Object... params) {
        super("invoiceNotPayable", params);
    }
}
