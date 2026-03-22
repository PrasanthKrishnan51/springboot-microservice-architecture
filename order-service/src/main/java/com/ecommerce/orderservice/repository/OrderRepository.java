package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.dao.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OrderRepository extends MongoRepository<Order, String> {
    Page<Order> findByUserId(String userId, Pageable pageable);

    Optional<Order> findByIdAndUserId(String id, String userId);

    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);
}
