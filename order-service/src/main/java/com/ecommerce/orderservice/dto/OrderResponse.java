package com.ecommerce.orderservice.dto;

import com.ecommerce.orderservice.dao.Order;
import com.ecommerce.orderservice.dao.OrderItem;
import com.ecommerce.orderservice.dao.ShippingAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String id;
    private String userId;
    private String correlationId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private Order.OrderStatus status;
    private ShippingAddress shippingAddress;
    private String paymentId;
    private String trackingNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
