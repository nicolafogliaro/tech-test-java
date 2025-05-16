package com.nicolafogliaro.orderservice.api.mapper;

import com.nicolafogliaro.orderservice.api.dto.order.OrderResponse;
import com.nicolafogliaro.orderservice.api.model.order.Order;

import java.util.Objects;

/**
 * Mapper Utility
 * Below is a basic, static-mapped utility.
 * If you prefer, you can replace this with a framework like MapStruct for larger projects.
 */
public class OrderMapper {

    public static OrderResponse toDto(Order order) {

        if (Objects.isNull(order)) {
            return null;
        }

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getDescription(),
                order.getStatus(),
                order.getTotalAmount(),
                OrderItemMapper.toDtoList(order.getOrderItems()),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

}
