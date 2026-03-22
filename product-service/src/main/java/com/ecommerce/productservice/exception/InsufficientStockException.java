package com.ecommerce.productservice.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String m) {
        super(m);
    }
}
