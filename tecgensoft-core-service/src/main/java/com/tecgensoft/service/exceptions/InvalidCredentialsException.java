package com.tecgensoft.service.exceptions;
import org.springframework.security.authentication.BadCredentialsException;

public class InvalidCredentialsException extends BadCredentialsException {
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
