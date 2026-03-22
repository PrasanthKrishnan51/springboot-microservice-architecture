package com.ecommerce.orderservice.exception;
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String m) { super(m); }
}
