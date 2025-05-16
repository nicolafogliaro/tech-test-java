package com.nicolafogliaro.orderservice.api.repository;

import com.nicolafogliaro.orderservice.api.model.order.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Note: JpaSpecificationExecutor<Order> to support dynamic filtering and searching.
 */
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    /**
     * Retrieves an Order by ID with its associated OrderItems and Products eagerly fetched.
     * This avoids lazy-loading issues when those relations are needed immediately.
     */
    @EntityGraph(attributePaths = {"orderItems.product"})
    Optional<Order> findById(Long id);

    @EntityGraph(attributePaths = {"orderItems.product"})
    Page<Order> findAll(Pageable pageable);

//    @Query("SELECT o FROM Order o JOIN FETCH o.orderItems")
//    List<Order> findAllWithOrderItems();

    //    TODO
//    @Query("SELECT o FROM Order o WHERE LOWER(o.description) LIKE LOWER(CONCAT('%', :word, '%'))")
//    List<Order> findByDescriptionContaining(@Param("word") String word);

}
