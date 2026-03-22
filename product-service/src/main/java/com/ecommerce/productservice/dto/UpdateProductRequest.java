package com.ecommerce.productservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateProductRequest {
    @Size(max = 200)
    private String name;
    @Size(max = 2000)
    private String description;
    private String category;
    private String brand;
    @DecimalMin("0.01")
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private List<String> imageUrls;
    private List<String> tags;
}
