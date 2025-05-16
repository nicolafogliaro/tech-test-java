package com.nicolafogliaro.orderservice.api.dto.orderitem;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Represents the response for an individual item within an order.
 * This record provides details about the product, quantity, and pricing for a specific order line.
 */
@Schema(description = "Represents an individual item within an order, detailing product, quantity, and pricing.")
public record OrderItemResponse(

        @Schema(description = "Unique identifier for the order item.", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Unique identifier of the product associated with this order item.", example = "789", requiredMode = Schema.RequiredMode.REQUIRED)
        Long productId,

        @Schema(description = "Name of the product.", example = "Wireless Mouse", requiredMode = Schema.RequiredMode.REQUIRED)
        String productName,

        @Schema(description = "Brief description of the product.", example = "Ergonomic wireless optical mouse", requiredMode = Schema.RequiredMode.NOT_REQUIRED) // Assuming description can sometimes be optional or missing
        String productDescription,

        @Schema(description = "Quantity of this product ordered.", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer quantity,

        @Schema(description = "Price of a single unit of the product.", example = "25.99", type = "number", format = "double", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal unitPrice,

        @Schema(description = "Total price for this order item (quantity * unitPrice).", example = "51.98", type = "number", format = "double", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal subtotalPrice) {}