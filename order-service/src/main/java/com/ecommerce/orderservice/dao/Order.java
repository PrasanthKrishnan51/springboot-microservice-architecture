package com.ecommerce.orderservice.dao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {
    @Id
    private String id;
    @Indexed
    private String userId;
    @Indexed
    private String correlationId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private ShippingAddress shippingAddress;
    private String paymentId;
    private String trackingNumber;
    private String cancellationReason;
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum OrderStatus {
        PENDING, PAYMENT_PENDING, CONFIRMED,
        PROCESSING, SHIPPED, DELIVERED, CANCELLED, REFUNDED
    }
}
