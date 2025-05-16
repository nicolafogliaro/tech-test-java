package com.nicolafogliaro.orderservice.api.repository;

import com.nicolafogliaro.orderservice.api.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByProductId(Long productId);

    boolean existsByProductId(Long productId);

}
