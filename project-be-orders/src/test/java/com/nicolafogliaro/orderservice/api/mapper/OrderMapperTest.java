package com.nicolafogliaro.orderservice.api.mapper;

import com.nicolafogliaro.orderservice.api.dto.orderitem.OrderItemResponse;
import com.nicolafogliaro.orderservice.api.model.OrderItem;
import com.nicolafogliaro.orderservice.api.model.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderMapperTest {

    @Test
    void toOrderItemDtoList_shouldMapOrderItemsToOrderItemDtos() {

        Product product1 = Product.builder()
                .id(1L)
                .name("Product 1")
                .build();

        Product product2 = Product.builder()
                .id(2L)
                .name("Product 2")
                .build();

        // Arrange: Create dummy OrderItem entities
        OrderItem orderItem1 = OrderItem.builder()
                .id(101L)
                .product(product1)
                .quantity(2)
                .unitPrice(new BigDecimal("50.00"))
                .build();

        OrderItem orderItem2 = OrderItem.builder()
                .id(102L)
                .product(product2)
                .quantity(1)
                .unitPrice(new BigDecimal("30.00"))
                .build();

        // Act: Call the toOrderItemDtoList method
        List<OrderItemResponse> dtos = OrderItemMapper.toDtoList(List.of(orderItem1, orderItem2));

        // Assert: Verify the results
        assertEquals(2, dtos.size()); // Verify correct number of DTOs were created

        // Assert DTO 1
        OrderItemResponse dto1 = dtos.get(0);
        assertEquals(101L, dto1.id());
        assertEquals(1L, dto1.productId());
        assertEquals("Product 1", dto1.productName());
        assertEquals(2, dto1.quantity());
        assertEquals(new BigDecimal("50.00"), dto1.unitPrice());
        assertEquals(new BigDecimal("100.00"), dto1.subtotalPrice());

        // Assert DTO 2
        OrderItemResponse dto2 = dtos.get(1);
        assertEquals(102L, dto2.id());
        assertEquals(2L, dto2.productId());
        assertEquals("Product 2", dto2.productName());
        assertEquals(1, dto2.quantity());
        assertEquals(new BigDecimal("30.00"), dto2.unitPrice());
        assertEquals(new BigDecimal("30.00"), dto2.subtotalPrice());
    }

}