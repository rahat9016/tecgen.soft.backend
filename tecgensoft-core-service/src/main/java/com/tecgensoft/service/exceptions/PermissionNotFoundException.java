package com.tecgensoft.service.exceptions;

import com.tecgensoft.common.exceptions.ResourceNotFoundException;

public class PermissionNotFoundException extends ResourceNotFoundException {
    public PermissionNotFoundException(Object ...params){
        super("permissionNotFound", params);
    }
}
