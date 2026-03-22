package com.ecommerce.orderservice.exception;
public class OrderCancellationException extends RuntimeException {
    public OrderCancellationException(String m) { super(m); }
}
