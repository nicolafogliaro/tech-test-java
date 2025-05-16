package com.nicolafogliaro.orderservice.api.service;

import com.nicolafogliaro.orderservice.api.dto.product.CreateProductRequest;
import com.nicolafogliaro.orderservice.api.dto.product.ProductResponse;
import com.nicolafogliaro.orderservice.api.dto.product.UpdateProductRequest;
import com.nicolafogliaro.orderservice.api.model.Product;

import java.util.List;

public interface ProductService {

    List<ProductResponse> searchProductsByName(String name);

    List<ProductResponse> getAllProducts();

    ProductResponse getProductById(Long id);

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse updateProduct(Long id, UpdateProductRequest request);

    Product decrementStock(Long id, int quantity);

    Product incrementStock(Long id, int quantity);

    void updateStock(Long productId, int quantityChange);

    boolean checkStockAvailability(Long productId, int requestedQuantity);

    void deleteProduct(Long id);
}
