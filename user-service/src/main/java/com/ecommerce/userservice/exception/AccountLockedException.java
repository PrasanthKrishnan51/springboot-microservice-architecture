package com.ecommerce.userservice.exception;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String m) {
        super(m);
    }
}
