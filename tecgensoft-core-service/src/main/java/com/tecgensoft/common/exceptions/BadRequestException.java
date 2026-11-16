/*
 * Copyright (c) 2026 FamilieMunshi. All rights reserved.
 * Author: Minhazur Rahman <minhazur>
 */
package com.tecgensoft.common.exceptions;

public class BadRequestException extends BusinessException {

    public BadRequestException(String messageKey) {
        super(messageKey);
    }

    public BadRequestException(String messageKey, Object... messageParams) {
        super(messageKey, messageParams);
    }
}