package com.nicolafogliaro.orderservice.api.controller;

import com.nicolafogliaro.orderservice.api.dto.product.CreateProductRequest;
import com.nicolafogliaro.orderservice.api.dto.product.ProductResponse;
import com.nicolafogliaro.orderservice.api.dto.product.UpdateProductRequest;
import com.nicolafogliaro.orderservice.api.service.impl.ProductServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * http://localhost:8080/order-service/api/v1/products
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product API", description = "Endpoints for managing products, including creation, retrieval, updating, and deletion.")
public class ProductController {

    private final ProductServiceImpl productService;

    /**
     * GET /api/v1/products
     * Retrieves all products.
     *
     * @return List of ProductDto objects.
     */
    @GetMapping
    @Operation(summary = "Get all products", description = "Retrieve all products in the system.")
    @ApiResponse(responseCode = "200", description = "List of all products retrieved successfully.")
    public List<ProductResponse> getAllProducts() {
        log.info(">>> [{}#getAllProducts]", ProductController.class.getSimpleName());
        List<ProductResponse> res = productService.getAllProducts();
        log.info("<<< [{}#getOrderById] <--- res: {}", OrderController.class.getSimpleName(), res);
        return res;
    }

    /**
     * GET /api/v1/products/{id}
     * Retrieves a product by ID.
     *
     * @param id The product ID.
     * @return The ProductDto for the specified ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieve a specific product by providing its ID.")
    @ApiResponse(responseCode = "200", description = "Product found and details retrieved successfully.")
    @ApiResponse(responseCode = "404", description = "Product with specified ID not found.")
    public ProductResponse getProductById(@PathVariable Long id) {
        log.info(">>> [{}#getProductById] --> id: {}", ProductController.class.getSimpleName(), id);
        ProductResponse res = productService.getProductById(id);
        log.info("<<< [{}#getProductById] ---> id: {} | <--- res: {}", OrderController.class.getSimpleName(), id, res);
        return res;
    }

    /**
     * POST /api/v1/products
     * Creates a new product.
     *
     * @param request The ProductRequest.
     * @return The created product.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // Explicit set of status
    @Operation(summary = "Create a product", description = "Create a new product in the system.")
    @ApiResponse(responseCode = "201", description = "Product created successfully.")
    public ProductResponse createProduct(@Valid @RequestBody CreateProductRequest request) {
        log.info(">>> [{}#createProduct] --> req: {}", ProductController.class.getSimpleName(), request);
        ProductResponse res = productService.createProduct(request);
        log.info("<<< [{}#createProduct] --> req: {} | <--- res: {}", ProductController.class.getSimpleName(), request, res);
        return res;
    }

    /**
     * PUT /api/v1/products/{id}
     * Updates an existing product.
     *
     * @param id      The product ID.
     * @param request The ProductRequest with updated data.
     * @return The updated product.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a product", description = "Update the details of an existing product.")
    @ApiResponse(responseCode = "200", description = "Product updated successfully.")
    @ApiResponse(responseCode = "404", description = "Product with specified ID not found.")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        log.info(">>> [{}#updateProduct] --> id: {}, req: {}", ProductController.class.getSimpleName(), id, request);
        ProductResponse res = productService.updateProduct(id, request);
        log.info("<<< [{}#createProduct] --> req: {} | <--- res: {}", ProductController.class.getSimpleName(), request, res);
        return res;
    }

    /**
     * DELETE /api/v1/products/{id}
     * Deletes a product by its ID.
     *
     * @param id The product ID.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)

    @Operation(summary = "Delete a product", description = "Delete a product from the system.")
    @ApiResponse(responseCode = "204", description = "Product deleted successfully.")
    @ApiResponse(responseCode = "404", description = "Product with specified ID not found.")
    public void deleteProduct(@PathVariable Long id) {
        log.info(">>> [{}#deleteProduct] --> id: {}", ProductController.class.getSimpleName(), id);
        productService.deleteProduct(id);
        log.info("<<< [{}#deleteProduct] --> id: {}", ProductController.class.getSimpleName(), id);
    }

}