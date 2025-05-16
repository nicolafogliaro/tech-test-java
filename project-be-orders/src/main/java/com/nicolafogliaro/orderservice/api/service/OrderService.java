package com.nicolafogliaro.orderservice.api.service;

import com.nicolafogliaro.orderservice.api.dto.order.OrderRequest;
import com.nicolafogliaro.orderservice.api.dto.order.OrderResponse;
import com.nicolafogliaro.orderservice.api.dto.order.UpdateOrderRequest;
import com.nicolafogliaro.orderservice.api.exception.ConcurrencyConflictException;
import com.nicolafogliaro.orderservice.api.exception.InsufficientStockException;
import com.nicolafogliaro.orderservice.api.exception.OrderNotFoundException;

public interface OrderService {

    OrderResponse getOrderById(Long orderId);

    OrderResponse createOrder(OrderRequest request);

    OrderResponse updateOrder(Long orderId, UpdateOrderRequest updateRequest)
            throws OrderNotFoundException, InsufficientStockException, ConcurrencyConflictException;

    void deleteOrder(Long orderId) throws OrderNotFoundException, ConcurrencyConflictException;
}
