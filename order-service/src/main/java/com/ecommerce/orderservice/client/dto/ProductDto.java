package com.ecommerce.orderservice.client.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private String id;
    private String name;
    private String category;
    private BigDecimal price;
    private int stock;
    private String status;
}
