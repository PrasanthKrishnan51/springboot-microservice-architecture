package com.ecommerce.orderservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestControllerAdvice @Slf4j
public class GlobalExceptionHandler {
    record ApiError(LocalDateTime ts, int status, String error, String msg, String traceId, String corrId) {}

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiError> notFound(OrderNotFoundException ex) { return b(HttpStatus.NOT_FOUND, ex.getMessage()); }
    @ExceptionHandler(OrderCancellationException.class)
    public ResponseEntity<ApiError> cancel(OrderCancellationException ex) { return b(HttpStatus.BAD_REQUEST, ex.getMessage()); }
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ApiError> svcDown(ServiceUnavailableException ex) { return b(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage()); }
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiError> stock(InsufficientStockException ex) { return b(HttpStatus.CONFLICT, ex.getMessage()); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> generic(Exception ex) { log.error("Unhandled", ex); return b(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error"); }

    private ResponseEntity<ApiError> b(HttpStatus s, String msg) {
        return ResponseEntity.status(s).body(new ApiError(LocalDateTime.now(), s.value(), s.getReasonPhrase(), msg, MDC.get("traceId"), MDC.get("correlationId")));
    }
}
