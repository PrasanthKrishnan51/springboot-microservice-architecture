package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dao.Order;
import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order API", description = "Order placement and management")
public class OrderController {

    private final OrderService service;

    @PostMapping
    @Operation(summary = "Place a new order")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody CreateOrderRequest req,
            @Parameter(description = "User placing the order", example = "USR-1001")
            @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "Correlation ID for tracing requests", example = "corr-12345")
            @RequestHeader(value = "X-Correlation-Id", required = false) String corrId) {

        log.info("Creating order for user {} corrId={}", userId, corrId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(req, userId, corrId));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> getById(
            @Parameter(description = "Order ID", example = "ORD-1001")
            @PathVariable String orderId,
            @Parameter(description = "User requesting the order", example = "USR-1001")
            @RequestHeader("X-User-Id") String userId) {

        log.info("Fetching order {} for user {}", orderId, userId);

        return ResponseEntity.ok(service.getById(orderId, userId));
    }

    @GetMapping("/my")
    @Operation(summary = "Get current user's orders")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Page<OrderResponse>> myOrders(
            @Parameter(description = "User ID", example = "USR-1001")
            @RequestHeader("X-User-Id") String userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("Fetching orders for user {}", userId);

        return ResponseEntity.ok(service.getByUser(userId, pageable));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get orders by status (admin)")
    public ResponseEntity<Page<OrderResponse>> byStatus(
            @Parameter(description = "Order status", example = "SHIPPED")
            @PathVariable Order.OrderStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Fetching orders by status {}", status);

        return ResponseEntity.ok(service.getByStatus(status, pageable));
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Update order status")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<OrderResponse> updateStatus(
            @Parameter(description = "Order ID", example = "ORD-1001")
            @PathVariable String orderId,
            @Parameter(description = "New order status", example = "SHIPPED")
            @RequestParam Order.OrderStatus status,
            @Parameter(description = "Optional reason for update")
            @RequestParam(required = false) String reason) {

        log.info("Updating status for order {} to {}", orderId, status);

        return ResponseEntity.ok(service.updateStatus(orderId, status, reason));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<OrderResponse> cancel(
            @Parameter(description = "Order ID", example = "ORD-1001")
            @PathVariable String orderId,
            @Parameter(description = "User requesting cancellation", example = "USR-1001")
            @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "Cancellation reason", example = "Customer request")
            @RequestParam(defaultValue = "Customer request") String reason) {

        log.info("Cancelling order {} for user {}", orderId, userId);

        return ResponseEntity.ok(service.cancel(orderId, userId, reason));
    }
}