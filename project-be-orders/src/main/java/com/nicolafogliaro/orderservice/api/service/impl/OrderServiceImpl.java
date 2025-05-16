package com.nicolafogliaro.orderservice.api.service.impl;

import com.nicolafogliaro.orderservice.api.dto.order.OrderRequest;
import com.nicolafogliaro.orderservice.api.dto.order.OrderResponse;
import com.nicolafogliaro.orderservice.api.dto.order.UpdateOrderRequest;
import com.nicolafogliaro.orderservice.api.dto.orderitem.OrderItemRequest;
import com.nicolafogliaro.orderservice.api.exception.ConcurrencyConflictException;
import com.nicolafogliaro.orderservice.api.exception.InsufficientStockException;
import com.nicolafogliaro.orderservice.api.exception.OrderNotFoundException;
import com.nicolafogliaro.orderservice.api.mapper.OrderItemMapper;
import com.nicolafogliaro.orderservice.api.mapper.OrderMapper;
import com.nicolafogliaro.orderservice.api.model.OrderItem;
import com.nicolafogliaro.orderservice.api.model.Product;
import com.nicolafogliaro.orderservice.api.model.order.Order;
import com.nicolafogliaro.orderservice.api.model.order.OrderStatus;
import com.nicolafogliaro.orderservice.api.repository.OrderRepository;
import com.nicolafogliaro.orderservice.api.repository.ProductRepository;
import com.nicolafogliaro.orderservice.api.service.OrderService;
import com.nicolafogliaro.orderservice.api.service.ProductService;
import com.nicolafogliaro.orderservice.api.util.MyCollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.nicolafogliaro.orderservice.api.config.CacheConfig.ORDER_CACHE_NAME;

@Service
@Transactional(readOnly = true)
@CacheConfig(cacheNames = ORDER_CACHE_NAME) // Specifies the default cache name for all cache operations in this class
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;


    @Override
    @Cacheable(key = "#id") // Uses "orders" cache name from @CacheConfig
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + id));
        return OrderMapper.toDto(order);
    }


    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Override
    @CachePut(key = "#result.id") // Puts the result into "orders" cache. Assumes OrderResponse has an 'id' field.
    public OrderResponse createOrder(OrderRequest request) {

        log.info(">>> [{}#createOrder] --> req: {}", getClass().getSimpleName(), request);

        Order newOrder = Order.builder()
                .customerId(request.customerId())
                .description(request.orderDescription())
                .status(Objects.requireNonNullElse(request.status(), OrderStatus.PENDING))
                .totalAmount(BigDecimal.ZERO)
                .orderItems(new ArrayList<>())
                .build();

        for (OrderItemRequest itemRequest : request.items()) {

            if (itemRequest.quantity() <= 0) {
                log.error("*** OrderItemRequest {} has an invalid quantity {}!", itemRequest, itemRequest.quantity());
                throw new IllegalStateException("Invalid quantity for product " + itemRequest.productId());
            }

            final Product product = productService.decrementStock(itemRequest.productId(), itemRequest.quantity());

            if (Objects.isNull(product.getPrice())) {
                // This should ideally not happen if products are always saved with prices.
                // If it can, it's a data integrity issue or a sign the product entity is not fully loaded.
                log.error("*** Product {} has a null price!", product.getId());
                throw new IllegalStateException("Product " + product.getId() + " has a null price.");
            }

            newOrder.getOrderItems().add(OrderItemMapper.toEntity(itemRequest, newOrder, product));
        }

        // Calculate total amount
        newOrder.calculateTotalAmount();

        Order saved = orderRepository.save(newOrder);

        OrderResponse res = OrderMapper.toDto(saved);

        log.info("<<< [{}#createOrder] --> req: {} | <--- res: {}", OrderServiceImpl.class.getSimpleName(), request, res);
        return res;
    }


    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Override
    @CachePut(key = "#id") // Updates the entry in "orders" cache for the given id
    public OrderResponse updateOrder(Long id, UpdateOrderRequest request) throws OrderNotFoundException, InsufficientStockException, ConcurrencyConflictException {

        log.info(">>> [{}#updateOrder] --> id: {}, req: {}", getClass().getSimpleName(), id, request);

        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + id));

        // Basic field updates
        Optional.ofNullable(request.customerId()).ifPresent(existingOrder::setCustomerId);
        Optional.ofNullable(request.orderDescription()).ifPresent(existingOrder::setDescription);
        Optional.ofNullable(request.status()).ifPresent(existingOrder::setStatus);

        // Calculate stockQuantity diffs
        Map<Long, Integer> oldQuantities = existingOrder.getOrderItems()
                .stream()
                .collect(Collectors.toMap(item -> item.getProduct().getId(), OrderItem::getQuantity));


        Map<Long, Integer> newQuantities = new HashMap<>();

        List<OrderItem> updatedItems = new ArrayList<>();

        if (MyCollectionUtils.nonEmpty(request.items())) {

            for (OrderItemRequest item : request.items()) {

                if (item.quantity() <= 0) {
                    throw new IllegalArgumentException("Invalid quantity: " + item.quantity());
                }

                Product product = productRepository.findById(item.productId())
                        .orElseThrow(() -> new IllegalArgumentException("Product not found for ID: " + item.productId()));

                newQuantities.put(item.productId(), item.quantity());

                updatedItems.add(OrderItem.builder()
                        .order(existingOrder)
                        .product(product)
                        .quantity(item.quantity())
                        .unitPrice(product.getPrice())
                        .build());
            }
        }

        // Apply stockQuantity changes
        oldQuantities.forEach((productId, originalQty) -> {
            int newQty = newQuantities.getOrDefault(productId, 0);
            if (originalQty > newQty) {
                productService.incrementStock(productId, originalQty - newQty); // restore
            }
        });

        newQuantities.forEach((productId, newQty) -> {
            int originalQty = oldQuantities.getOrDefault(productId, 0);
            if (newQty > originalQty) {
                productService.decrementStock(productId, newQty - originalQty); // consume
            }
        });

        existingOrder.getOrderItems().clear();
        existingOrder.getOrderItems().addAll(updatedItems);
        existingOrder.calculateTotalAmount();

        OrderResponse res = OrderMapper.toDto(orderRepository.save(existingOrder));

        log.info("<<< [{}#updateOrder] --> id: {}, req: {} | <--- res: {}", OrderServiceImpl.class.getSimpleName(), id, request, res);
        return res;
    }


    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Override
    @CacheEvict(key = "#id") // Removes the entry from "orders" cache for the given orderId
    public void deleteOrder(Long id) throws OrderNotFoundException {

        Order orderToDelete = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + id));

        for (OrderItem item : orderToDelete.getOrderItems()) {

            if (Objects.nonNull(item.getProduct())) {
                productService.decrementStock(item.getProduct().getId(), item.getQuantity());
            }
        }
        orderRepository.deleteById(id);
    }

}
