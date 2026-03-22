package com.ecommerce.orderservice.exception;
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String m) { super(m); }
}
