package com.ecommerce.apigateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/fallback")
@Slf4j
@Tag(name = "Fallback API", description = "Fallback endpoints for unavailable downstream services")
public class FallbackController {

    @GetMapping("/products")
    @Operation(summary = "Product service fallback")
    public ResponseEntity<FallbackResponse> products() {
        log.warn("Circuit breaker open — product-service");
        return fallback("product-service");
    }

    @GetMapping("/orders")
    @Operation(summary = "Order service fallback")
    public ResponseEntity<FallbackResponse> orders() {
        log.warn("Circuit breaker open — order-service");
        return fallback("order-service");
    }

    @GetMapping("/users")
    @Operation(summary = "User service fallback")
    public ResponseEntity<FallbackResponse> users() {
        log.warn("Circuit breaker open — user-service");
        return fallback("user-service");
    }

    private ResponseEntity<FallbackResponse> fallback(String svc) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new FallbackResponse(
                        LocalDateTime.now().toString(),
                        503,
                        "Service Unavailable",
                        svc + " is temporarily unavailable. Please retry.",
                        svc
                ));
    }

    public record FallbackResponse(
            String timestamp,
            int status,
            String error,
            String message,
            String service
    ) {}

}