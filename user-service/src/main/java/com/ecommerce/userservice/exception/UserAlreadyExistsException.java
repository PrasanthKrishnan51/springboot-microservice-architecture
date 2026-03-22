package com.ecommerce.userservice.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String m) {
        super(m);
    }
}
