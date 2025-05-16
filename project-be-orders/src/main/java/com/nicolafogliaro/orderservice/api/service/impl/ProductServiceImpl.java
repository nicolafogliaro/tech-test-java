package com.nicolafogliaro.orderservice.api.service.impl;

import com.nicolafogliaro.orderservice.api.dto.product.CreateProductRequest;
import com.nicolafogliaro.orderservice.api.dto.product.ProductResponse;
import com.nicolafogliaro.orderservice.api.dto.product.UpdateProductRequest;
import com.nicolafogliaro.orderservice.api.exception.InsufficientStockException;
import com.nicolafogliaro.orderservice.api.exception.ProductBadRequestException;
import com.nicolafogliaro.orderservice.api.exception.ProductNotFoundException;
import com.nicolafogliaro.orderservice.api.mapper.ProductMapper;
import com.nicolafogliaro.orderservice.api.model.Product;
import com.nicolafogliaro.orderservice.api.repository.OrderItemRepository;
import com.nicolafogliaro.orderservice.api.repository.ProductRepository;
import com.nicolafogliaro.orderservice.api.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.nicolafogliaro.orderservice.api.config.CacheConfig.PRODUCTS_CACHE_NAME;
import static com.nicolafogliaro.orderservice.api.config.CacheConfig.PRODUCT_CACHE_NAME;

/**
 *
 */
@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;


    /**
     * Searches for products containing the specified name (case-insensitive).
     *
     * @param name Partial or full product name.
     * @return List of matching products.
     */
    @Override
    public List<ProductResponse> searchProductsByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name).stream()
                .map(ProductMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all products from the repository.
     * This operation is read-only.
     *
     * @return A list of all products. Returns an empty list if no products exist.
     */
    @Cacheable(value = PRODUCTS_CACHE_NAME)
    @Override
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(ProductMapper::toDto)
                .collect(Collectors.toList());
    }


    /**
     * Retrieves a single product by its unique identifier.
     *
     * @param id The ID of the product.
     * @return The corresponding Product.
     * @throws ProductNotFoundException if no product with the given ID exists.
     * @throws NullPointerException     if the provided ID is null.
     */
    @Cacheable(value = PRODUCT_CACHE_NAME, key = "#id")
    @Override
    public ProductResponse getProductById(Long id) {
        Product product = getProductOrThrow(Objects.requireNonNull(id, "Product ID cannot be null."));
        return ProductMapper.toDto(product);
    }

    /**
     * Helper method to retrieve product or throw exception.
     *
     * @param id Product ID
     * @return Product entity
     * @throws ProductNotFoundException if product not found
     */
    private Product getProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product with ID " + id + " not found."));
    }

    /**
     * Creates a new product based on the provided ProductRequest.
     *
     * @param request The ProductRequest containing product details.
     * @return The created product as a ProductDto.
     */
    @Caching(
            put = {@CachePut(cacheNames = PRODUCT_CACHE_NAME, key = "#result.id")}, // Assumes ProductResponse has an 'id' field
            evict = {@CacheEvict(cacheNames = PRODUCTS_CACHE_NAME, allEntries = true)}
    )
    @Transactional
    @Override
    public ProductResponse createProduct(CreateProductRequest request) {
        Product product = ProductMapper.toEntity(request);
        Product saved = productRepository.save(product);
        log.info("Product created with ID {}", saved.getId());
        return ProductMapper.toDto(saved);
    }

    /**
     * Updates an existing product by its ID using data from the request.
     *
     * @param id      The ID of the product to update.
     * @param request The ProductRequest containing updated product details.
     * @return The updated product as a ProductDto.
     */
    @Caching(
            put = {@CachePut(cacheNames = PRODUCT_CACHE_NAME, key = "#id")},
            evict = {@CacheEvict(cacheNames = PRODUCTS_CACHE_NAME, allEntries = true)}
    )
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Override
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = getProductOrThrow(id);
        ProductMapper.updateEntityFromDto(request, product);
        Product updated = productRepository.save(product);
        log.info("Product updated with ID {}", id);
        return ProductMapper.toDto(updated);
    }


    /**
     * Decrements stockQuantity for a product when an order is placed.
     *
     * @param productId Product ID
     * @param quantity  Quantity to decrement
     * @return The updated product entity.
     */
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = PRODUCT_CACHE_NAME, key = "#productId"),
                    @CacheEvict(cacheNames = PRODUCTS_CACHE_NAME, allEntries = true)
            }
    )
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Override
    public Product decrementStock(Long productId, int quantity) {

        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        if (product.getStockQuantity() < quantity) {
            throw new InsufficientStockException("Insufficient stockQuantity for product ID " + productId);
        }

        product.setStockQuantity(product.getStockQuantity() - quantity);
        log.info("Decremented stockQuantity for product ID {} by {}", productId, quantity);
        return productRepository.save(product);
    }

    /**
     * Increments stockQuantity for a product when an order is canceled.
     *
     * @param productId Product ID
     * @param quantity  Quantity to increment
     * @return The updated product entity.
     */
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = PRODUCT_CACHE_NAME, key = "#productId"),
                    @CacheEvict(cacheNames = PRODUCTS_CACHE_NAME, allEntries = true)
            }
    )
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Override
    public Product incrementStock(Long productId, int quantity) {

        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        product.setStockQuantity(product.getStockQuantity() + quantity);
        log.info("Incremented stockQuantity for product ID {} by {}", productId, quantity);
        return productRepository.save(product);
    }

    @Caching(
            evict = {
                    @CacheEvict(cacheNames = PRODUCT_CACHE_NAME, key = "#productId"),
                    @CacheEvict(cacheNames = PRODUCTS_CACHE_NAME, allEntries = true)
            }
    )
    @Transactional
    @Override
    public void updateStock(Long productId, int quantityChange) {

        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));

        int updatedStock = product.getStockQuantity() + quantityChange;

        if (updatedStock < 0) {
            throw new InsufficientStockException("Insufficient stockQuantity for product: " + product.getName());
        }

        product.setStockQuantity(updatedStock);
        // No explicit save needed — managed entity will be persisted
    }

    @Override
    public boolean checkStockAvailability(Long productId, int requestedQuantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));

        return product.getStockQuantity() >= requestedQuantity;
    }

    /**
     * Deletes a product by its ID.
     *
     * @param id Product ID
     */
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = PRODUCT_CACHE_NAME, key = "#id"),
                    @CacheEvict(cacheNames = PRODUCTS_CACHE_NAME, allEntries = true)
            }
    )
    @Transactional
    @Override
    public void deleteProduct(Long id) {

        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException("Product with ID " + id + " does not exist.");
        }

        boolean isReferenced = orderItemRepository.existsByProductId(id);
        if (isReferenced) {
            throw new ProductBadRequestException("Cannot delete product. It is referenced in an order.");
        }

        productRepository.deleteById(id);

        log.info("Deleted product with ID {}", id);
    }

}
