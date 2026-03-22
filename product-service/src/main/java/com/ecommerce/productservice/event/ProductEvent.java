package com.ecommerce.productservice.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductEvent {
    private String eventType;   // PRODUCT_CREATED | PRODUCT_UPDATED | STOCK_UPDATED | PRODUCT_DELETED
    private String productId;
    private String name;
    private String category;
    private BigDecimal price;
    private Integer stock;
    private String sellerId;
    private String correlationId;
    private Instant timestamp;
}
