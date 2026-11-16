package com.tecgensoft.service.exceptions;

import com.tecgensoft.common.exceptions.ResourceNotFoundException;

public class BranchNotFoundException extends ResourceNotFoundException {
    public BranchNotFoundException(Object ...params){
        super("branchNotFound", params);
    }
}
