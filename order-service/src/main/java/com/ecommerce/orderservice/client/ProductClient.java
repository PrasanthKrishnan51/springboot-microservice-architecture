package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.client.dto.ProductDto;
import com.ecommerce.orderservice.client.fallback.ProductClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "product-service",
        fallback = ProductClientFallback.class
)
public interface ProductClient {

    @GetMapping("/api/v1/products/{id}")
    ProductDto getProduct(@PathVariable String id);

    @PatchMapping("/api/v1/products/{id}/stock")
    ProductDto updateStock(@PathVariable String id, @RequestBody Object req);
}
