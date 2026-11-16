package com.tecgensoft.service.exceptions;

import com.tecgensoft.common.exceptions.ConflictException;

public class UserEmailAlreadyExistsException extends ConflictException {
    public UserEmailAlreadyExistsException(Object ...params){
        super("userEmailExists", params);
    }
}
