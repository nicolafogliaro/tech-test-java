package com.nicolafogliaro.orderservice.api.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Product Data Transfer Object (DTO) used for creating products.
 * Represents the input payload for creating or updating products in the system.
 *
 * @param name          The name of the product (required).
 * @param description   A brief description of the product (optional).
 * @param price         The price of the product (required).
 * @param stockQuantity The initial stock quantity quantity of the product (required).
 */
@Schema(description = "Request object for creating or updating a product.")
public record CreateProductRequest(

        @NotEmpty(message = "Product name cannot be empty")
        @Size(max = 255, message = "The product name must not exceed 255 characters.")
        @Schema(description = "Name of the product.", example = "Tablet", required = true)
        String name,

        @Size(max = 1000, message = "The product description must not exceed 1000 characters.")
        @Schema(description = "Short description of the product.", example = "A lightweight and portable tablet.")
        String description,

        @Positive(message = "Product price must be greater than 0.")
        @Schema(description = "Price of the product.", example = "499.99", required = true)
        BigDecimal price,

        @PositiveOrZero(message = "Stock quantity must be 0 or greater.")
        @Schema(description = "Initial or updated stockQuantity quantity of the product.", example = "50", required = true)
        Integer stockQuantity
) {}

