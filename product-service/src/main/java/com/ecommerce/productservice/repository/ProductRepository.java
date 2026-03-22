package com.ecommerce.productservice.repository;

import com.ecommerce.productservice.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.math.BigDecimal;

public interface ProductRepository extends MongoRepository<Product, String> {
    Page<Product> findByCategory(String category, Pageable pageable);

    Page<Product> findBySellerId(String sellerId, Pageable pageable);

    Page<Product> findByStatus(Product.ProductStatus status, Pageable pageable);

    @Query("{ 'category': ?0, 'price': { $gte: ?1, $lte: ?2 }, 'status': 'ACTIVE' }")
    Page<Product> findByCategoryAndPriceRange(String category,
                                              BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    @Query("{ 'stock': { $lt: ?0 }, 'status': 'ACTIVE' }")
    Page<Product> findLowStock(int threshold, Pageable pageable);
}
