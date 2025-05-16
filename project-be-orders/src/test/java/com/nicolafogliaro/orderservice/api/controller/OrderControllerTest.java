package com.nicolafogliaro.orderservice.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicolafogliaro.orderservice.api.dto.order.OrderRequest;
import com.nicolafogliaro.orderservice.api.dto.order.OrderResponse;
import com.nicolafogliaro.orderservice.api.dto.order.UpdateOrderRequest;
import com.nicolafogliaro.orderservice.api.dto.orderitem.OrderItemRequest;
import com.nicolafogliaro.orderservice.api.dto.orderitem.OrderItemResponse;
import com.nicolafogliaro.orderservice.api.exception.OrderNotFoundException;
import com.nicolafogliaro.orderservice.api.model.order.OrderStatus;
import com.nicolafogliaro.orderservice.api.service.OrderSearchService;
import com.nicolafogliaro.orderservice.api.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Make sure OrderController, OrderService, OrderSearchService, DTOs, and ResourceNotFoundException
// are accessible (e.g., defined in the same file for this example, or imported).

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private OrderSearchService orderSearchService; // Must be mocked as it's a dependency

    @Autowired
    private ObjectMapper objectMapper;

    private OrderResponse sampleOrderResponse;
    private OrderItemResponse sampleOrderItemResponse;

    @BeforeEach
    void setUp() {
        // For Spring Security or other filters, setup might be needed:
        // MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        sampleOrderItemResponse = new OrderItemResponse(
                1L,
                101L,
                "Test Product",
                "Test Product description",
                2,
                new BigDecimal("25.00"),
                new BigDecimal("50.00")
        );

        sampleOrderResponse = new OrderResponse(
                1L,
                1L,
                "Test Customer",
                OrderStatus.PENDING,
                new BigDecimal("50.00"),
                Collections.singletonList(sampleOrderItemResponse),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void getOrderById_whenOrderExists_shouldReturnOrderResponse() throws Exception {
        Long orderId = 1L;
        when(orderService.getOrderById(orderId)).thenReturn(sampleOrderResponse);

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(orderId.intValue())))
                .andExpect(jsonPath("$.description", is(sampleOrderResponse.description())))
                .andExpect(jsonPath("$.items[0].productName", is(sampleOrderItemResponse.productName())));

        verify(orderService).getOrderById(orderId);
    }

    @Test
    void getOrderById_whenOrderNotFound_shouldReturnNotFound() throws Exception {
        Long orderId = 2L;
        when(orderService.getOrderById(orderId)).thenThrow(new OrderNotFoundException("Order not found with ID: " + orderId));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isNotFound()); // Assuming you have a @ControllerAdvice for ResourceNotFoundException

        verify(orderService).getOrderById(orderId);
    }

    @Test
    void createOrder_withValidRequest_shouldReturnCreatedOrder() throws Exception {

        OrderItemRequest itemRequest = new OrderItemRequest(
                101L,
                2);

        OrderRequest orderRequest = new OrderRequest(
                1L,
                "New order",
                null,
                Collections.singletonList(itemRequest)
        );

        when(orderService.createOrder(any(OrderRequest.class))).thenReturn(sampleOrderResponse);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(sampleOrderResponse.id().intValue())))
                .andExpect(jsonPath("$.description", is(sampleOrderResponse.description())));

        verify(orderService).createOrder(eq(orderRequest));
    }

    @Test
    void createOrder_withInvalidRequest_missingCustomerId_shouldReturnBadRequest() throws Exception {
        // customerId is null, items list is empty (violates @NotEmpty)
        OrderRequest invalidOrderRequest = new OrderRequest(null, "Invalid order", null, Collections.emptyList());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidOrderRequest)))
                .andExpect(status().isBadRequest()); // Due to @Valid and bean validation annotations

        verifyNoInteractions(orderService); // Service should not be called
    }

    @Test
    void createOrder_withInvalidRequest_emptyItems_shouldReturnBadRequest() throws Exception {

        OrderRequest invalidOrderRequest = new OrderRequest(
                1L,
                "Invalid order",
                null,
                Collections.emptyList());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidOrderRequest)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }


    @Test
    void updateOrder_whenOrderExistsAndValidRequest_shouldReturnUpdatedOrder() throws Exception {

        final Long orderId = 1L;

        String expectedUpdatedDescription = "Updated description";

        UpdateOrderRequest updateRequest = new UpdateOrderRequest(
                null,
                expectedUpdatedDescription,
                OrderStatus.PROCESSING,
                null
        );

        OrderResponse updatedResponse = new OrderResponse(
                orderId,
                1L,
                "Updated description",
                OrderStatus.PROCESSING,
                new BigDecimal("50.00"),
                Collections.singletonList(sampleOrderItemResponse),
                sampleOrderResponse.createdAt(),
                LocalDateTime.now());

        when(orderService.updateOrder(eq(orderId), any(UpdateOrderRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/orders/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(orderId.intValue())))
                .andExpect(jsonPath("$.description", is(expectedUpdatedDescription)))
                .andExpect(jsonPath("$.status", is(OrderStatus.PROCESSING.toString())));

        verify(orderService).updateOrder(eq(orderId), eq(updateRequest));
    }

    @Test
    void updateOrder_whenOrderNotFound_shouldReturnNotFound() throws Exception {
        Long orderId = 2L;
        UpdateOrderRequest updateRequest = new UpdateOrderRequest(null, "Update for non-existent", null, null);
        when(orderService.updateOrder(eq(orderId), any(UpdateOrderRequest.class)))
                .thenThrow(new OrderNotFoundException("Order not found with ID: " + orderId));

        mockMvc.perform(put("/api/v1/orders/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound()); // Assuming @ControllerAdvice

        verify(orderService).updateOrder(eq(orderId), eq(updateRequest));
    }

    @Test
    void updateOrder_withInvalidItemInRequest_shouldReturnBadRequest() throws Exception {
        Long orderId = 1L;
        // Invalid item: quantity is 0, which violates @Min(1) on OrderItemRequest.quantity
        OrderItemRequest invalidItem = new OrderItemRequest(
                202L,
                0);

        UpdateOrderRequest updateRequestWithInvalidItem = new UpdateOrderRequest(
                null,
                "Update with invalid item",
                null,
                Collections.singletonList(invalidItem)
        );

        mockMvc.perform(put("/api/v1/orders/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequestWithInvalidItem)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void deleteOrder_whenOrderExists_shouldReturnNoContent() throws Exception {
        Long orderId = 1L;
        doNothing().when(orderService).deleteOrder(orderId);

        mockMvc.perform(delete("/api/v1/orders/{id}", orderId))
                .andExpect(status().isNoContent());

        verify(orderService).deleteOrder(orderId);
    }

    @Test
    void deleteOrder_whenOrderNotFound_shouldReturnNotFound() throws Exception {
        Long orderId = 2L;
        doThrow(new OrderNotFoundException("Order not found with ID: " + orderId))
                .when(orderService).deleteOrder(orderId);

        mockMvc.perform(delete("/api/v1/orders/{id}", orderId))
                .andExpect(status().isNotFound()); // Assuming @ControllerAdvice

        verify(orderService).deleteOrder(orderId);
    }
}

