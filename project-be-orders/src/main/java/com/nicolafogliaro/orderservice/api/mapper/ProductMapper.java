package com.nicolafogliaro.orderservice.api.mapper;

import com.nicolafogliaro.orderservice.api.dto.product.CreateProductRequest;
import com.nicolafogliaro.orderservice.api.dto.product.ProductResponse;
import com.nicolafogliaro.orderservice.api.dto.product.UpdateProductRequest;
import com.nicolafogliaro.orderservice.api.model.Product;

/**
 * Mapper Utility
 * Basic, static-mapped utility.
 */
public final class ProductMapper {

    public static ProductResponse toDto(Product entity) {
        if (entity == null) {
            return null;
        }
        return new ProductResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getStockQuantity(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static Product toEntity(CreateProductRequest dto) {
        if (dto == null) {
            return null;
        }
        return Product.builder()
                .name(dto.name())
                .description(dto.description())
                .price(dto.price())
                .stockQuantity(dto.stockQuantity())
                .build();
    }

    public static void updateEntityFromDto(UpdateProductRequest dto, Product entity) {

        if (dto == null || entity == null) {
            return;
        }

        if (dto.name() != null) {
            entity.setName(dto.name());
        }

        if (dto.description() != null) {
            entity.setDescription(dto.description());
        }

        if (dto.price() != null) { // Added price
            entity.setPrice(dto.price());
        }

        if (dto.stockQuantity() != null) {
            entity.setStockQuantity(dto.stockQuantity());
        }

    }

    private ProductMapper() {}
}

