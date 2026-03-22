package com.ecommerce.productservice.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class StockUpdateRequest {
    @Min(0)
    private int quantity;
    private Operation operation;

    public enum Operation {ADD, SUBTRACT, SET}
}
