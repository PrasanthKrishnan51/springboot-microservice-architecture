package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.client.dto.ProductDto;
import com.ecommerce.orderservice.dao.Order;
import com.ecommerce.orderservice.dao.OrderItem;
import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.event.OrderEvent;
import com.ecommerce.orderservice.exception.InsufficientStockException;
import com.ecommerce.orderservice.exception.OrderCancellationException;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.mapper.OrderMapper;
import com.ecommerce.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository repo;
    private final OrderMapper mapper;
    private final ProductClient productClient;
    private final KafkaTemplate<String, OrderEvent> kafka;

    // ── CREATE ───────────────────────────────────────────────────────────────
    public OrderResponse create(CreateOrderRequest req, String userId, String corrId) {
        log.info("Creating order userId={} correlationId={} itemCount={}", userId, corrId, req.getItems().size());

        // Resolve products via Feign (REST call to product-service)
        List<OrderItem> items = req.getItems().stream().map(i -> {
            ProductDto p = productClient.getProduct(i.getProductId());
            if (p.getStock() < i.getQuantity())
                throw new InsufficientStockException("Insufficient stock for product: " + i.getProductId());
            return OrderItem.builder()
                    .productId(i.getProductId()).productName(p.getName()).category(p.getCategory())
                    .quantity(i.getQuantity()).unitPrice(p.getPrice())
                    .totalPrice(p.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .build();
        }).collect(Collectors.toList());

        BigDecimal total = items.stream().map(OrderItem::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .userId(userId)
                .correlationId(corrId != null ? corrId : UUID.randomUUID().toString())
                .items(items).totalAmount(total)
                .status(Order.OrderStatus.PENDING)
                .shippingAddress(mapper.toShippingAddress(req.getShippingAddress()))
                .build();

        Order saved = repo.save(order);
        publishEvent("ORDER_CREATED", saved);
        log.info("Order created orderId={} totalAmount={}", saved.getId(), total);
        return mapper.toResponse(saved);
    }

    // ── READ ─────────────────────────────────────────────────────────────────
    public OrderResponse getById(String orderId, String userId) {
        return mapper.toResponse(repo.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId)));
    }

    public Page<OrderResponse> getByUser(String userId, Pageable p) {
        return repo.findByUserId(userId, p).map(mapper::toResponse);
    }

    public Page<OrderResponse> getByStatus(Order.OrderStatus status, Pageable p) {
        return repo.findByStatus(status, p).map(mapper::toResponse);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    public OrderResponse updateStatus(String orderId, Order.OrderStatus newStatus, String reason) {
        Order o = repo.findById(orderId).orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        log.info("Status update orderId={} {} → {}", orderId, o.getStatus(), newStatus);
        o.setStatus(newStatus);
        if (reason != null) o.setCancellationReason(reason);
        Order saved = repo.save(o);
        publishEvent("ORDER_STATUS_UPDATED", saved);
        return mapper.toResponse(saved);
    }

    public OrderResponse cancel(String orderId, String userId, String reason) {
        Order o = repo.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        if (o.getStatus() == Order.OrderStatus.SHIPPED || o.getStatus() == Order.OrderStatus.DELIVERED)
            throw new OrderCancellationException("Cannot cancel order in status: " + o.getStatus());
        o.setStatus(Order.OrderStatus.CANCELLED);
        o.setCancellationReason(reason);
        Order saved = repo.save(o);
        publishEvent("ORDER_CANCELLED", saved);
        log.info("Order cancelled orderId={} reason={}", orderId, reason);
        return mapper.toResponse(saved);
    }

    // ── Private ───────────────────────────────────────────────────────────────
    private void publishEvent(String type, Order o) {
        OrderEvent evt = OrderEvent.builder()
                .eventType(type)
                .orderId(o.getId())
                .userId(o.getUserId())
                .totalAmount(o.getTotalAmount())
                .status(o.getStatus().name())
                .correlationId(o.getCorrelationId())
                .timestamp(Instant.now()).build();
        kafka.send("order-events", o.getId(), evt)
                .whenComplete((r, ex) -> {
                    if (ex != null) log.error("Kafka publish failed event={} orderId={}", type, o.getId(), ex);
                });
    }
}
