package com.nicolafogliaro.orderservice.api.mapper;

import com.nicolafogliaro.orderservice.api.dto.orderitem.OrderItemRequest;
import com.nicolafogliaro.orderservice.api.dto.orderitem.OrderItemResponse;
import com.nicolafogliaro.orderservice.api.model.OrderItem;
import com.nicolafogliaro.orderservice.api.model.Product;
import com.nicolafogliaro.orderservice.api.model.order.Order;
import com.nicolafogliaro.orderservice.api.model.order.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderItemMapperTest {

    private OrderItem orderItem;
    private Product product;
    private Order order;

    private OrderItemRequest validRequest;


    @BeforeEach
    void setUp() {
        // Setup Product
        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(100.00))
                .stockQuantity(10)
                .build();

        // Setup Order
        order = Order.builder()
                .id(1L)
                .customerId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(200.00))
                .build();

        // Setup OrderItem
        orderItem = OrderItem.builder()
                .id(1L)
                .order(order)
                .product(product)
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(100.00))
                .build();

        validRequest = new OrderItemRequest(1L, 2);

    }

    @Test
    @DisplayName("Should map OrderItem to OrderItemResponse correctly")
    void toDto_ShouldMapCorrectly() {
        // Act
        OrderItemResponse response = OrderItemMapper.toDto(orderItem);

        // Assert
        assertThat(response)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.id()).isEqualTo(orderItem.getId());
                    assertThat(r.productId()).isEqualTo(product.getId());
                    assertThat(r.productName()).isEqualTo(product.getName());
                    assertThat(r.productDescription()).isEqualTo(product.getDescription());
                    assertThat(r.quantity()).isEqualTo(orderItem.getQuantity());
                    assertThat(r.unitPrice()).isEqualTo(orderItem.getUnitPrice());
                    assertThat(r.subtotalPrice())
                            .isEqualTo(orderItem.getUnitPrice()
                                    .multiply(BigDecimal.valueOf(orderItem.getQuantity())));
                });
    }

    @Test
    @DisplayName("Should handle null OrderItem")
    void toDto_ShouldHandleNull() {
        // Act
        OrderItemResponse response = OrderItemMapper.toDto(null);

        // Assert
        assertThat(response).isNull();
    }

    @Test
    @DisplayName("Should map list of OrderItems to list of OrderItemResponses")
    void toDtoList_ShouldMapListCorrectly() {
        // Arrange
        List<OrderItem> orderItems = Arrays.asList(orderItem, createAnotherOrderItem());

        // Act
        List<OrderItemResponse> responses = OrderItemMapper.toDtoList(orderItems);

        // Assert
        assertThat(responses)
                .isNotNull()
                .hasSize(2)
                .allSatisfy(response -> {
                    assertThat(response.productId()).isNotNull();
                    assertThat(response.productName()).isNotNull();
                    assertThat(response.quantity()).isPositive();
                    assertThat(response.subtotalPrice()).isPositive();
                });
    }

    @Test
    @DisplayName("Should handle null list")
    void toDtoList_ShouldHandleNullList() {
        // Act
        List<OrderItemResponse> responses = OrderItemMapper.toDtoList(null);

        // Assert
        assertThat(responses)
                .isNotNull()
                .isEmpty();
    }

    @Test
    @DisplayName("Should handle empty list")
    void toDtoList_ShouldHandleEmptyList() {
        // Act
        List<OrderItemResponse> responses = OrderItemMapper.toDtoList(Collections.emptyList());

        // Assert
        assertThat(responses)
                .isNotNull()
                .isEmpty();
    }


    @Test
    @DisplayName("Should calculate subtotal price correctly")
    void toDto_ShouldCalculateSubtotalPriceCorrectly() {
        // Arrange
        orderItem.setQuantity(3);
        orderItem.setUnitPrice(BigDecimal.valueOf(50.00));

        // Act
        OrderItemResponse response = OrderItemMapper.toDto(orderItem);

        // Assert
        assertThat(response.subtotalPrice())
                .isEqualTo(BigDecimal.valueOf(150.00));
    }

    @Test
    @DisplayName("Should handle OrderItem with minimum values")
    void toDto_ShouldHandleMinimumValues() {
        // Arrange
        orderItem.setQuantity(1);
        orderItem.setUnitPrice(BigDecimal.ZERO);

        // Act
        OrderItemResponse response = OrderItemMapper.toDto(orderItem);

        // Assert
        assertThat(response.subtotalPrice())
                .isEqualTo(BigDecimal.ZERO);
    }

    private OrderItem createAnotherOrderItem() {
        Product anotherProduct = Product.builder()
                .id(2L)
                .name("Another Product")
                .description("Another Description")
                .price(BigDecimal.valueOf(150.00))
                .stockQuantity(5)
                .build();

        return OrderItem.builder()
                .id(2L)
                .order(order)
                .product(anotherProduct)
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(150.00))
                .build();
    }

    @Test
    void toDtoList_withEmptyList_shouldReturnEmptyList() {
        // Act: Call with an empty list
        List<OrderItemResponse> dtos = OrderItemMapper.toDtoList(List.of());

        // Assert: Verify that the result is empty
        assertTrue(dtos.isEmpty(), "The result should be an empty list");
    }

    @Test
    void toDtoList_withNullInput_shouldReturnEmptyList() {
        // Act: Call with a null list
        List<OrderItemResponse> dtos = OrderItemMapper.toDtoList(null);

        // Assert: Verify that the result is empty
        assertTrue(dtos.isEmpty(), "The result should be an empty list when input is null");
    }

    @Test
    @DisplayName("Should map to OrderItem entity correctly with valid inputs")
    void toEntity_ShouldMapCorrectly_WithValidInputs() {
        // Act
        OrderItem result = OrderItemMapper.toEntity(validRequest, order, product);
        // Assert
        assertThat(result)
                .isNotNull()
                .satisfies(orderItem -> {
                    assertThat(orderItem.getOrder()).isEqualTo(order);
                    assertThat(orderItem.getProduct()).isEqualTo(product);
                    assertThat(orderItem.getUnitPrice()).isEqualTo(product.getPrice());
                    assertThat(orderItem.getQuantity()).isEqualTo(validRequest.quantity());
                });
    }

    @Test
    @DisplayName("Should throw NullPointerException when request is null")
    void toEntity_ShouldThrowException_WhenRequestIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> OrderItemMapper.toEntity(null, order, product))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should throw NullPointerException when product is null")
    void toEntity_ShouldThrowException_WhenProductIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> OrderItemMapper.toEntity(validRequest, order, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should map correctly with minimum valid quantity")
    void toEntity_ShouldMapCorrectly_WithMinimumQuantity() {
        // Arrange
        OrderItemRequest request = new OrderItemRequest(1L, 1);
        // Act
        OrderItem result = OrderItemMapper.toEntity(request, order, product);
        // Assert
        assertThat(result.getQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should map correctly with large quantity")
    void toEntity_ShouldMapCorrectly_WithLargeQuantity() {
        // Arrange
        OrderItemRequest request = new OrderItemRequest(1L, 999);
        // Act
        OrderItem result = OrderItemMapper.toEntity(request, order, product);
        // Assert
        assertThat(result.getQuantity()).isEqualTo(999);
    }

    @Test
    @DisplayName("Should map correctly with different product prices")
    void toEntity_ShouldMapCorrectly_WithDifferentProductPrices() {
        // Arrange
        Product expensiveProduct = Product.builder()
                .id(2L)
                .price(BigDecimal.valueOf(999.99))
                .build();
        // Act
        OrderItem result = OrderItemMapper.toEntity(validRequest, order, expensiveProduct);
        // Assert
        assertThat(result.getUnitPrice())
                .isEqualTo(BigDecimal.valueOf(999.99));
    }

    @Test
    @DisplayName("Should preserve order and product references")
    void toEntity_ShouldPreserveReferences() {
        // Act
        OrderItem result = OrderItemMapper.toEntity(validRequest, order, product);
        // Assert
        assertThat(result.getOrder())
                .isSameAs(order);
        assertThat(result.getProduct())
                .isSameAs(product);
    }

    @Test
    @DisplayName("Should map correctly with zero price product")
    void toEntity_ShouldMapCorrectly_WithZeroPriceProduct() {
        // Arrange
        Product freeProduct = Product.builder()
                .id(3L)
                .price(BigDecimal.ZERO)
                .build();
        // Act
        OrderItem result = OrderItemMapper.toEntity(validRequest, order, freeProduct);
        // Assert
        assertThat(result.getUnitPrice())
                .isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should create new instance for each conversion")
    void toEntity_ShouldCreateNewInstance_ForEachConversion() {
        // Act
        OrderItem result1 = OrderItemMapper.toEntity(validRequest, order, product);
        OrderItem result2 = OrderItemMapper.toEntity(validRequest, order, product);
        // Assert
        assertThat(result1)
                .isNotSameAs(result2)
                .usingRecursiveComparison()
                .isEqualTo(result2);
    }
}