package com.tecgensoft.service.exceptions;

import com.tecgensoft.common.exceptions.ConflictException;

public class BranchNameAlreadyExistsException extends ConflictException {
    public BranchNameAlreadyExistsException(Object ...params){
        super("branchNameExists", params);
    }
}
