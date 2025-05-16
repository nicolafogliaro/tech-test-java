package com.nicolafogliaro.orderservice.api.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A Data Transfer Object (DTO) representing product details for API responses.
 * This ensures the internal entity structure is not directly exposed to clients.
 *
 * @param id            The unique identifier for the product.
 * @param name          The product's name.
 * @param description   A short description of the product.
 * @param price         The price of the product.
 * @param stockQuantity The current stock quantity of the product.
 */
@Schema(description = "Response object representing a product.")
public record ProductResponse(

        @Schema(description = "Unique identifier of the product.", example = "1")
        Long id,

        @Schema(description = "Name of the product.", example = "Laptop")
        String name,

        @Schema(description = "Short description of the product.", example = "A high-performance laptop with 16GB RAM.")
        String description,

        @Schema(description = "Price of the product.", example = "999.99")
        BigDecimal price,

        @Schema(description = "Current stock quantity of the product.", example = "100")
        Integer stockQuantity,

        @Schema(description = "Timestamp when the product was created", example = "2025-05-14T13:45:30")
        LocalDateTime createdAt,

        @Schema(description = "Timestamp when the product was last updated", example = "2025-05-14T14:15:22")
        LocalDateTime updatedAt
) implements Serializable {}
