package com.ecommerce.productservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product {

    @Id
    private String id;
    @TextIndexed
    private String name;
    @TextIndexed
    private String description;
    @Indexed
    private String category;
    @Indexed
    private String brand;
    @Indexed
    private String sellerId;

    private BigDecimal price;
    private BigDecimal discountedPrice;
    private Integer stock;
    private Double rating;
    private Integer reviewCount;
    private List<String> imageUrls;
    private List<String> tags;
    private ProductStatus status;

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum ProductStatus {ACTIVE, INACTIVE, OUT_OF_STOCK, DISCONTINUED}
}
