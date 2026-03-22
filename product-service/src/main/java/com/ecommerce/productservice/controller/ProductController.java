package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.dto.CreateProductRequest;
import com.ecommerce.productservice.dto.ProductResponse;
import com.ecommerce.productservice.dto.StockUpdateRequest;
import com.ecommerce.productservice.dto.UpdateProductRequest;
import com.ecommerce.productservice.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product API", description = "Product catalogue & inventory management")
public class ProductController {

    private final ProductService service;

    @PostMapping
    @Operation(summary = "Create a new product")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody CreateProductRequest req,
            @Parameter(description = "Seller ID creating the product", example = "USR-1001")
            @RequestHeader("X-User-Id") String sellerId,
            @Parameter(description = "Correlation ID for request tracing", example = "corr-12345")
            @RequestHeader(value = "X-Correlation-Id", required = false) String corrId) {

        log.info("Create product request received from seller {}", sellerId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(req, sellerId, corrId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> getById(
            @Parameter(description = "Product ID", example = "PROD-1001")
            @PathVariable String id) {

        log.info("Fetching product {}", id);

        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    @Operation(summary = "List all products (paginated)")
    public ResponseEntity<Page<ProductResponse>> getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("Fetching paginated products");

        return ResponseEntity.ok(service.getAll(pageable));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get products by category")
    public ResponseEntity<Page<ProductResponse>> byCategory(
            @Parameter(description = "Product category", example = "electronics")
            @PathVariable String category,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Fetching products by category {}", category);

        return ResponseEntity.ok(service.getByCategory(category, pageable));
    }

    @GetMapping("/seller/{sellerId}")
    @Operation(summary = "Get products by seller")
    public ResponseEntity<Page<ProductResponse>> bySeller(
            @Parameter(description = "Seller ID", example = "USR-1001")
            @PathVariable String sellerId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Fetching products for seller {}", sellerId);

        return ResponseEntity.ok(service.getBySeller(sellerId, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product details")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProductResponse> update(
            @Parameter(description = "Product ID", example = "PROD-1001")
            @PathVariable String id,
            @Valid @RequestBody UpdateProductRequest req,
            @RequestHeader(value = "X-Correlation-Id", required = false) String corrId) {

        log.info("Updating product {}", id);

        return ResponseEntity.ok(service.update(id, req, corrId));
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "Update product stock (ADD / SUBTRACT / SET)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProductResponse> updateStock(
            @Parameter(description = "Product ID", example = "PROD-1001")
            @PathVariable String id,
            @Valid @RequestBody StockUpdateRequest req,
            @RequestHeader(value = "X-Correlation-Id", required = false) String corrId) {

        log.info("Updating stock for product {}", id);

        return ResponseEntity.ok(service.updateStock(id, req, corrId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a product")
    @ApiResponse(responseCode = "204", description = "Product deleted successfully")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Product ID", example = "PROD-1001")
            @PathVariable String id,
            @RequestHeader(value = "X-Correlation-Id", required = false) String corrId) {

        log.info("Deleting product {}", id);
        service.delete(id, corrId);

        return ResponseEntity.noContent().build();
    }
}