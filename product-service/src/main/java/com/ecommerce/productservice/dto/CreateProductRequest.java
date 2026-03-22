package com.ecommerce.productservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateProductRequest {
    @NotBlank
    @Size(max = 200)
    private String name;
    @NotBlank
    @Size(max = 2000)
    private String description;
    @NotBlank
    private String category;
    private String brand;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;
    @Min(0)
    private int stock;
    private List<String> imageUrls;
    private List<String> tags;
}
