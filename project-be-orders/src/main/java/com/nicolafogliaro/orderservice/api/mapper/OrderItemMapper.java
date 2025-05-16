package com.nicolafogliaro.orderservice.api.mapper;

import com.nicolafogliaro.orderservice.api.dto.orderitem.OrderItemRequest;
import com.nicolafogliaro.orderservice.api.dto.orderitem.OrderItemResponse;
import com.nicolafogliaro.orderservice.api.model.OrderItem;
import com.nicolafogliaro.orderservice.api.model.Product;
import com.nicolafogliaro.orderservice.api.model.order.Order;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Mapper Utility
 */
@UtilityClass// I used here Lombok in order to show this alternative to make class immutable.
// I prefer Manual definition because more simple and without dependencies.
public class OrderItemMapper {

    public static OrderItemResponse toDto(OrderItem orderItem) {

        if (Objects.isNull(orderItem)) {
            return null;
        }

        return new OrderItemResponse(
                orderItem.getId(),
                orderItem.getProduct() != null ? orderItem.getProduct().getId() : null,
                orderItem.getProduct() != null ? orderItem.getProduct().getName() : "N/A",
                orderItem.getProduct() != null ? orderItem.getProduct().getDescription() : "N/A",
                orderItem.getQuantity(),
                orderItem.getUnitPrice(),
                calculateSubtotalPrice(orderItem.getQuantity(), orderItem.getUnitPrice())
        );
    }

    public static List<OrderItemResponse> toDtoList(List<OrderItem> items) {

        if (Objects.isNull(items) || items.isEmpty()) {
            return Collections.emptyList();
        }

        return items.stream()
                .map(OrderItemMapper::toDto)
                .collect(Collectors.toList());
    }


    public static OrderItem toEntity(OrderItemRequest itemRequest,
                                     Order order,
                                     Product product) {
        return OrderItem.builder()
                .order(order)
                .product(product)
                .unitPrice(product.getPrice())
                .quantity(itemRequest.quantity())
                .build();
    }


    public static BigDecimal calculateSubtotalPrice(int quantity, BigDecimal unitPrice) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

}
