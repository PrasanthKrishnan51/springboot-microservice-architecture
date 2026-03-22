package com.ecommerce.orderservice.event.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class ProductEventHandler {

    /**
     * Listens to product-events published by product-service.
     * Useful for maintaining a local read-model / price cache.
     */
    @KafkaListener(
            topics = "product-events",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handle(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        String type = (String) event.getOrDefault("eventType", "UNKNOWN");
        String productId = (String) event.getOrDefault("productId", "n/a");

        log.info("ProductEvent received type={} productId={} topic={} partition={} offset={}",
                type, productId, topic, partition, offset);

        switch (type) {
            case "STOCK_UPDATED" -> handleStockUpdate(event);
            case "PRODUCT_DELETED" -> handleProductDeleted(productId);
            default -> log.debug("ProductEvent type={} — no handler registered", type);
        }
    }

    private void handleStockUpdate(Map<String, Object> event) {
        // TODO: update local product cache / invalidate order readiness checks
        log.debug("Stock update for productId={} newStock={}",
                event.get("productId"), event.get("stock"));
    }

    private void handleProductDeleted(String productId) {
        // TODO: mark any PENDING orders containing this product as needing review
        log.warn("Product deleted productId={} — reviewing pending orders", productId);
    }
}
