package com.nicolafogliaro.orderservice.api.dto.orderitem;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a request to add a specific product and its quantity to an order.
 * This record is used as a Data Transfer Object (DTO) in API requests
 * when creating or updating orders with line items.
 *
 * <p>Validation constraints ensure that the product ID is provided and the quantity
 * is at least one.</p>
 *
 * @param productId The unique identifier of the product.
 *                  This field is mandatory and cannot be null.
 * @param quantity  The number of units of the product to be ordered.
 *                  This field is mandatory and must be at least 1.
 */
@Schema(description = "Represents an item to be included in an order, specifying the product and quantity.")
public record OrderItemRequest(

        @Schema(description = "The unique identifier of the product to be ordered.", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Product ID cannot be null")
        Long productId,

        @Schema(description = "The number of units of the product to be ordered. Must be at least 1.", example = "2", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity) {
}