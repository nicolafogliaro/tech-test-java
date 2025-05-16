package com.nicolafogliaro.orderservice.api.dto.order;


import com.nicolafogliaro.orderservice.api.dto.orderitem.OrderItemResponse;
import com.nicolafogliaro.orderservice.api.model.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object (DTO) that represents an order response.
 * Contains all details of an order including its items, status, and timestamps.
 */
@Schema(description = "Representation of an order with its details")
public record OrderResponse(

        @Schema(description = "Unique identifier of the order", example = "1001")
        Long id,

        @Schema(description = "Identifier of the customer who placed this order", example = "5001")
        Long customerId,

        @Schema(description = "Text description or notes for the order", example = "Priority shipment for VIP customer")
        String description,

        @Schema(description = "Current status of the order", example = "PROCESSING",
                allowableValues = {"CREATED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"})
        OrderStatus status,

        @Schema(description = "Total monetary amount of the order", example = "129.99")
        BigDecimal totalAmount,

        @ArraySchema(schema = @Schema(implementation = OrderItemResponse.class))
        List<OrderItemResponse> items,

        @Schema(description = "Timestamp when the order was created", example = "2025-05-14T13:45:30")
        LocalDateTime createdAt,

        @Schema(description = "Timestamp when the order was last updated", example = "2025-05-14T14:15:22")
        LocalDateTime updatedAt
) implements Serializable {}