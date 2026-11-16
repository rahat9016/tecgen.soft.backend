package com.tecgensoft.service.exceptions;

import com.tecgensoft.common.exceptions.ResourceNotFoundException;

public class RoleNotFoundException  extends ResourceNotFoundException {
    public RoleNotFoundException(Object ...params){
        super("roleNotFound", params);
    }
}
