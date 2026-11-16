package com.tecgensoft.service.exceptions;

import com.tecgensoft.common.exceptions.BadRequestException;

public class InvalidUserCreationException extends BadRequestException {

    public InvalidUserCreationException(Object... params) {
        super("invalidUserCreation", params);
    }
}
