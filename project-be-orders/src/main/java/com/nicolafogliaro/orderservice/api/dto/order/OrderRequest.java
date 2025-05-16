package com.nicolafogliaro.orderservice.api.dto.order;


import com.nicolafogliaro.orderservice.api.dto.orderitem.OrderItemRequest;
import com.nicolafogliaro.orderservice.api.model.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Represents a request to create an order.
 * <p>
 * This record encapsulates all required information for order processing operations,
 * including customer identification, order details, status, and line items.
 * </p>
 */
@Schema(description = "Order creation request")
public record OrderRequest(

        @NotNull
        @Schema(description = "Unique identifier of the customer",
                example = "1001",
                required = true)
        Long customerId,

        @NotEmpty(message = "Order description cannot be empty")
        @Size(max = 500, message = "Description cannot exceed 500 characters")
        @Schema(description = "Description or notes for the order",
                example = "Rush delivery requested",
                required = true,
                minLength = 1,
                maxLength = 500)
        String orderDescription,

        @Schema(description = "Order status",
                example = "PENDING",
                defaultValue = "PENDING",
                implementation = OrderStatus.class)
        OrderStatus status,

        @NotEmpty(message = "Order must contain at least one item")
        @Size(min = 1, max = 100, message = "Order must have between 1 and 100 items")
        @ArraySchema(
                schema = @Schema(implementation = OrderItemRequest.class),
                minItems = 1,
                maxItems = 100,
                uniqueItems = false
        )
        @Schema(description = "Line items included in this order",
                required = true)
        List<@Valid OrderItemRequest> items
) {}