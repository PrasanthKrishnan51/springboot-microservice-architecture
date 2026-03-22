package com.ecommerce.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private String eventType;   // ORDER_CREATED | ORDER_UPDATED | ORDER_CANCELLED
    private String orderId;
    private String userId;
    private String userEmail;
    private BigDecimal totalAmount;
    private String status;
    private String correlationId;
    private Instant timestamp;
}
