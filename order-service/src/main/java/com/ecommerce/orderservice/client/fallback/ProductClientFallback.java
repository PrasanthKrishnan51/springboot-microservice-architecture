package com.ecommerce.orderservice.client.fallback;

import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.client.dto.ProductDto;
import com.ecommerce.orderservice.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProductClientFallback implements ProductClient {

    @Override
    public ProductDto getProduct(String id) {
        log.error("Fallback: product-service unavailable for productId={}", id);
        throw new ServiceUnavailableException("Product service is currently unavailable");
    }

    @Override
    public ProductDto updateStock(String id, Object req) {
        log.error("Fallback: cannot update stock for productId={}", id);
        throw new ServiceUnavailableException("Product service is currently unavailable");
    }
}
