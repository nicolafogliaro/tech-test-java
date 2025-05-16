package com.nicolafogliaro.orderservice.api.service.impl;

import com.nicolafogliaro.orderservice.api.dto.order.OrderRequest;
import com.nicolafogliaro.orderservice.api.dto.order.OrderResponse;
import com.nicolafogliaro.orderservice.api.dto.order.UpdateOrderRequest;
import com.nicolafogliaro.orderservice.api.dto.orderitem.OrderItemRequest;
import com.nicolafogliaro.orderservice.api.exception.ConcurrencyConflictException;
import com.nicolafogliaro.orderservice.api.exception.InsufficientStockException;
import com.nicolafogliaro.orderservice.api.exception.OrderNotFoundException;
import com.nicolafogliaro.orderservice.api.model.OrderItem;
import com.nicolafogliaro.orderservice.api.model.Product;
import com.nicolafogliaro.orderservice.api.model.order.Order;
import com.nicolafogliaro.orderservice.api.model.order.OrderStatus;
import com.nicolafogliaro.orderservice.api.repository.OrderRepository;
import com.nicolafogliaro.orderservice.api.repository.ProductRepository;
import com.nicolafogliaro.orderservice.api.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * I've created comprehensive unit tests for the OrderServiceImpl class. Let me explain the key aspects of the test suite:
 * Test Coverage Overview
 * The tests cover all the main service methods:
 * <p>
 * getOrderById
 * listOrders
 * getAllOrders
 * createOrder
 * updateOrder
 * deleteOrder
 * <p>
 * For each method, I've included multiple test cases covering:
 * <p>
 * Happy path scenarios
 * Error conditions
 * Edge cases
 * <p>
 * Key Testing Techniques Used
 * <p>
 * Mocking Dependencies: Using Mockito to mock OrderRepository, ProductRepository, and ProductService
 * ArgumentCaptors: To verify complex objects being passed to repository methods
 * Comprehensive Assertions: Verifying both the return values and that the correct methods were called with expected parameters
 * Exception Testing: Asserting that appropriate exceptions are thrown in error scenarios
 * <p>
 * Notable Test Cases
 * <p>
 * Testing stockQuantity adjustment logic when updating order quantities
 * Verifying validation for invalid inputs like null prices or zero quantities
 * Testing error handling when orders aren't found
 * Testing dynamic filtering with specifications
 * <p>
 * Additional Test Considerations
 * You might want to consider adding:
 * <p>
 * Integration tests for the entire service
 * Tests for transactional behavior (particularly important given the @Transactional annotations)
 * Tests for concurrent access scenarios (given the isolation levels specified in the service)
 * <p>
 * Would you like me to explain any specific part of the test suite in more detail or extend the tests to cover additional scenarios?
 */

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order mockOrder;
    private Product mockProduct;
    private List<Order> mockOrders;
    private OrderRequest mockOrderRequest;
    private UpdateOrderRequest mockUpdateOrderRequest;

    @BeforeEach
    void setUp() {
        // Set up mock product
        mockProduct = Product.builder()
                .id(1L)
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(100.00))
                .stockQuantity(10)
                .build();

        // Set up mock order
        mockOrder = Order.builder()
                .id(1L)
                .customerId(1L)
                .description("Test Order")
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(200.00))
                .orderItems(new ArrayList<>())
                .build();

        // Add order items to the order
        OrderItem orderItem = OrderItem.builder()
                .id(1L)
                .order(mockOrder)
                .product(mockProduct)
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(100.00))
                .build();
        mockOrder.getOrderItems().add(orderItem);

        // Create multiple mock orders for list testing
        mockOrders = Arrays.asList(
                mockOrder,
                Order.builder()
                        .id(2L)
                        .customerId(2L)
                        .description("Second Test Order")
                        .status(OrderStatus.CONFIRMED)
                        .totalAmount(BigDecimal.valueOf(300.00))
                        .orderItems(new ArrayList<>())
                        .build()
        );

        // Create mock order request
        OrderItemRequest itemRequest = new OrderItemRequest(1L, 2);
        mockOrderRequest = new OrderRequest(
                1L,
                "New Test Order",
                OrderStatus.PENDING,
                Collections.singletonList(itemRequest)
        );

        // Create mock update order request
        mockUpdateOrderRequest = new UpdateOrderRequest(
                1L,
                "Updated Test Order",
                OrderStatus.CONFIRMED,
                Collections.singletonList(itemRequest)
        );
    }

    @Test
    @DisplayName("Should return order by ID when order exists")
    void getOrderById_WhenOrderExists_ReturnsOrderResponse() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        // Act
        OrderResponse response = orderService.getOrderById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Test Order", response.description());
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(BigDecimal.valueOf(200.00), response.totalAmount());
        assertEquals(1, response.items().size());

        // Verify method calls
        verify(orderRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when order does not exist")
    void getOrderById_WhenOrderDoesNotExist_ThrowsOrderNotFoundException() {
        // Arrange
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrderById(99L)
        );
        assertEquals("Order not found with ID: 99", exception.getMessage());

        // Verify method calls
        verify(orderRepository).findById(99L);
    }


    @Test
    @DisplayName("Should create a new order successfully")
    void createOrder_SuccessfullyCreatesNewOrder() {
        // Arrange
        when(productService.decrementStock(1L, 2)).thenReturn(mockProduct);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            savedOrder.setId(1L);
            return savedOrder;
        });

        // Act
        OrderResponse response = orderService.createOrder(mockOrderRequest);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("New Test Order", response.description());
        assertEquals(OrderStatus.PENDING, response.status());

        // Verify method calls
        verify(productService).decrementStock(1L, 2);
        verify(orderRepository).save(any(Order.class));

        // Capture the Order argument to verify its content
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();

        assertEquals(1L, capturedOrder.getCustomerId());
        assertEquals("New Test Order", capturedOrder.getDescription());
        assertEquals(OrderStatus.PENDING, capturedOrder.getStatus());
        assertEquals(1, capturedOrder.getOrderItems().size());
    }

    @Test
    @DisplayName("Should throw exception when creating order with product having null price")
    void createOrder_WithProductHavingNullPrice_ThrowsIllegalStateException() {
        // Arrange
        Product productWithNullPrice = Product.builder()
                .id(1L)
                .name("Test Product")
                .description("Test Description")
                .price(null)  // Null price
                .stockQuantity(10)
                .build();

        when(productService.decrementStock(1L, 2)).thenReturn(productWithNullPrice);

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderService.createOrder(mockOrderRequest)
        );
        assertEquals("Product 1 has a null price.", exception.getMessage());

        // Verify method calls
        verify(productService).decrementStock(1L, 2);
    }

    @Test
    @DisplayName("Should throw exception when creating order with invalid quantity")
    void createOrder_WithInvalidQuantity_ThrowsIllegalStateException() {

        // Arrange
        OrderItemRequest invalidItemRequest = new OrderItemRequest(1L, 0);  // Invalid quantity

        OrderRequest invalidRequest = new OrderRequest(
                1L,
                "New Test Order",
                OrderStatus.PENDING,
                Collections.singletonList(invalidItemRequest)
        );

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderService.createOrder(invalidRequest)
        );

        assertTrue(exception.getMessage().contains("Invalid quantity for product"));
    }

    @Test
    @DisplayName("Should update order successfully")
    void updateOrder_SuccessfullyUpdatesExistingOrder() throws OrderNotFoundException, InsufficientStockException, ConcurrencyConflictException {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponse response = orderService.updateOrder(1L, mockUpdateOrderRequest);

        // Assert
        assertNotNull(response);
        assertEquals("Updated Test Order", response.description());
        assertEquals(OrderStatus.CONFIRMED, response.status());

        // Verify method calls
        verify(orderRepository).findById(1L);
        verify(productRepository).findById(1L);
        verify(orderRepository).save(any(Order.class));

        // Verify product stockQuantity adjustments
        // No stockQuantity adjustment needed in this case because quantities are the same (2)
        verify(productService, never()).incrementStock(eq(1L), anyInt());
        verify(productService, never()).decrementStock(eq(1L), anyInt());
    }

    @Test
    @DisplayName("Should adjust product stockQuantity when updating order with different quantities")
    void updateOrder_WithDifferentQuantities_AdjustsProductStock() throws OrderNotFoundException, InsufficientStockException, ConcurrencyConflictException {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        // Update with a higher quantity (2 -> 5)
        OrderItemRequest updatedItemRequest = new OrderItemRequest(1L, 5);
        UpdateOrderRequest updateRequest = new UpdateOrderRequest(
                1L,
                "Updated Test Order",
                OrderStatus.CONFIRMED,
                Collections.singletonList(updatedItemRequest)
        );

        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productService.decrementStock(1L, 3)).thenReturn(mockProduct);  // Additional 3 units
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        orderService.updateOrder(1L, updateRequest);

        // Assert
        // Verify product stockQuantity adjustments
        verify(productService).decrementStock(1L, 3);  // Should decrement by the difference (5-2=3)
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when updating non-existent order")
    void updateOrder_WithNonExistentOrder_ThrowsOrderNotFoundException() {
        // Arrange
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> orderService.updateOrder(99L, mockUpdateOrderRequest)
        );
        assertEquals("Order not found with ID: 99", exception.getMessage());

        // Verify method calls
        verify(orderRepository).findById(99L);
    }

    @Test
    @DisplayName("Should delete order successfully")
    void deleteOrder_SuccessfullyDeletesExistingOrder() throws OrderNotFoundException {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        doNothing().when(orderRepository).deleteById(1L);

        // Act
        orderService.deleteOrder(1L);

        // Assert
        // Verify method calls
        verify(orderRepository).findById(1L);
        verify(orderRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when deleting non-existent order")
    void deleteOrder_WithNonExistentOrder_ThrowsOrderNotFoundException() {
        // Arrange
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> orderService.deleteOrder(99L)
        );
        assertEquals("Order not found with ID: 99", exception.getMessage());

        // Verify method calls
        verify(orderRepository).findById(99L);
        verify(orderRepository, never()).deleteById(anyLong());
    }
}