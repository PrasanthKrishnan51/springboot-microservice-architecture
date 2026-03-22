package com.ecommerce.productservice.dto;

import com.ecommerce.productservice.domain.Product;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private String id;
    private String name;
    private String description;
    private String category;
    private String brand;
    private String sellerId;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private Integer stock;
    private Double rating;
    private Integer reviewCount;
    private List<String> imageUrls;
    private List<String> tags;
    private Product.ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
