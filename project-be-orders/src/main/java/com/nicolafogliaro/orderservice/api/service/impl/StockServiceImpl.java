package com.nicolafogliaro.orderservice.api.service.impl;

import com.nicolafogliaro.orderservice.api.exception.InsufficientStockException;
import com.nicolafogliaro.orderservice.api.exception.ProductNotFoundException;
import com.nicolafogliaro.orderservice.api.model.Product;
import com.nicolafogliaro.orderservice.api.repository.ProductRepository;
import com.nicolafogliaro.orderservice.api.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final ProductRepository productRepository;

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

        // No need to call save; managed entity will be updated on commit
    }


    @Override
    public boolean checkStockAvailability(Long productId, int requestedQuantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));

        return product.getStockQuantity() >= requestedQuantity;
    }

}