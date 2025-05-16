package com.nicolafogliaro.orderservice.api.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * All fields optional for PATCH-style updates
 *
 * @param name
 * @param description
 * @param price
 * @param stockQuantity
 */
public record UpdateProductRequest(

        @Schema(
                description = "New name of the product. If not provided, the name remains unchanged.",
                example = "Advanced Tablet"
        )
        @Size(max = 255, message = "The product name must not exceed 255 characters.")
        String name,

        @Schema(
                description = "New description of the product. If not provided, the description remains unchanged.",
                example = "A more advanced and lightweight tablet with extra features."
        )
        @Size(max = 1000, message = "The product description must not exceed 1000 characters.")
        String description,

        @Schema(
                description = "New price of the product. Must be greater than 0 if provided.",
                example = "599.99"
        )
        @Positive(message = "Product price must be greater than 0.")
        BigDecimal price,

        @Schema(
                description = "New stock quantity of the product. Must be 0 or greater if provided.",
                example = "50"
        )
        @PositiveOrZero(message = "Stock quantity must be 0 or greater.")
        Integer stockQuantity
) {}