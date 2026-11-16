package com.tecgensoft.service.exceptions;

import com.tecgensoft.common.exceptions.ResourceNotFoundException;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(Object ...params){
        super("userNotFound", params);
    }
}
