package com.ecommerce.productservice.service;

import com.ecommerce.productservice.domain.Product;
import com.ecommerce.productservice.dto.*;
import com.ecommerce.productservice.event.ProductEvent;
import com.ecommerce.productservice.exception.*;
import com.ecommerce.productservice.mapper.ProductMapper;
import com.ecommerce.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository repo;
    private final ProductMapper mapper;
    private final KafkaTemplate<String, ProductEvent> kafka;

    // ── CREATE ───────────────────────────────────────────────────────────────
    public ProductResponse create(CreateProductRequest req,
                                  String sellerId,
                                  String correlationId) {
        log.info("Creating product name={} sellerId={} correlationId={}", req.getName(), sellerId, correlationId);

        Product p = mapper.toEntity(req);
        p.setSellerId(sellerId);
        p.setRating(0.0);
        p.setReviewCount(0);
        p.setStatus(req.getStock() > 0 ? Product.ProductStatus.ACTIVE : Product.ProductStatus.OUT_OF_STOCK);
        if (p.getImageUrls() == null) p.setImageUrls(new ArrayList<>());
        if (p.getTags() == null) p.setTags(new ArrayList<>());

        Product saved = repo.save(p);
        publishEvent("PRODUCT_CREATED", saved, correlationId);
        log.info("Product created productId={}", saved.getId());
        return mapper.toResponse(saved);
    }

    // ── READ ─────────────────────────────────────────────────────────────────
    public ProductResponse getById(String id) {
        return mapper.toResponse(findOrThrow(id));
    }

    public Page<ProductResponse> getAll(Pageable pageable) {
        return repo.findAll(pageable).map(mapper::toResponse);
    }

    public Page<ProductResponse> getByCategory(String category, Pageable pageable) {
        return repo.findByCategory(category, pageable).map(mapper::toResponse);
    }

    public Page<ProductResponse> getBySeller(String sellerId, Pageable pageable) {
        return repo.findBySellerId(sellerId, pageable).map(mapper::toResponse);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    public ProductResponse update(String id, UpdateProductRequest req, String correlationId) {
        Product p = findOrThrow(id);
        mapper.updateFromRequest(req, p);
        Product saved = repo.save(p);
        publishEvent("PRODUCT_UPDATED", saved, correlationId);
        log.info("Product updated productId={}", id);
        return mapper.toResponse(saved);
    }

    // ── STOCK ─────────────────────────────────────────────────────────────────
    public ProductResponse updateStock(String id, StockUpdateRequest req, String correlationId) {
        Product p = findOrThrow(id);
        int newStock;
        switch (req.getOperation()) {
            case ADD -> newStock = p.getStock() + req.getQuantity();
            case SUBTRACT -> {
                if (p.getStock() < req.getQuantity())
                    throw new InsufficientStockException("Insufficient stock for product: " + id);
                newStock = p.getStock() - req.getQuantity();
            }
            default -> newStock = req.getQuantity();
        }
        p.setStock(newStock);
        p.setStatus(newStock > 0 ? Product.ProductStatus.ACTIVE : Product.ProductStatus.OUT_OF_STOCK);
        Product saved = repo.save(p);
        publishEvent("STOCK_UPDATED", saved, correlationId);
        log.info("Stock updated productId={} newStock={}", id, newStock);
        return mapper.toResponse(saved);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    public void delete(String id, String correlationId) {
        Product p = findOrThrow(id);
        p.setStatus(Product.ProductStatus.INACTIVE);
        repo.save(p);
        publishEvent("PRODUCT_DELETED", p, correlationId);
        log.info("Product soft-deleted productId={}", id);
    }

    // ── Internal ─────────────────────────────────────────────────────────────
    private Product findOrThrow(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));
    }

    private void publishEvent(String type, Product p, String correlationId) {
        ProductEvent evt = ProductEvent.builder()
                .eventType(type)
                .productId(p.getId())
                .name(p.getName())
                .category(p.getCategory())
                .price(p.getPrice())
                .stock(p.getStock())
                .sellerId(p.getSellerId())
                .correlationId(correlationId)
                .timestamp(Instant.now()).build();

        kafka.send("product-events", p.getId(), evt)
                .whenComplete((r, ex) -> {
                    if (ex != null) log.error("Kafka publish failed event={} productId={}", type, p.getId(), ex);
                    else log.debug("Kafka published event={} productId={}", type, p.getId());
                });
    }
}
