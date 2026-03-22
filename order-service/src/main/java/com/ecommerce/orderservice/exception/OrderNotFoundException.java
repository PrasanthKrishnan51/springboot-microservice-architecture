package com.ecommerce.orderservice.exception;
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String m) { super(m); }
}
