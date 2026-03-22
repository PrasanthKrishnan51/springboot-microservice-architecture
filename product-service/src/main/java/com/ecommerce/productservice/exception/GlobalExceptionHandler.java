package com.ecommerce.productservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.requests.ApiError;
import org.slf4j.MDC;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiError> notFound(ProductNotFoundException ex) {
        log.warn("Product not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiError> stock(InsufficientStockException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        List<String> errs = ex.getBindingResult().getFieldErrors()
                .stream().map(FieldError::getDefaultMessage).collect(Collectors.toList());
        return build(HttpStatus.BAD_REQUEST, String.join("; ", errs));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> generic(Exception ex) {
        log.error("Unhandled", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred");
    }

    private ResponseEntity<ApiError> build(HttpStatus s, String msg) {
        return ResponseEntity.status(s).body(new ApiError(
                LocalDateTime.now(), s.value(), s.getReasonPhrase(),
                msg, MDC.get("traceId"), MDC.get("correlationId")));
    }

    public record ApiError(LocalDateTime timestamp, int status, String error,
                           String message, String traceId, String correlationId) {
    }
}
