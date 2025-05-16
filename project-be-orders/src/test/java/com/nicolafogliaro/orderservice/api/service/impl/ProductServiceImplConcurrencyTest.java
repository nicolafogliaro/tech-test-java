package com.nicolafogliaro.orderservice.api.service.impl;

import com.nicolafogliaro.orderservice.api.exception.InsufficientStockException;
import com.nicolafogliaro.orderservice.api.model.Product;
import com.nicolafogliaro.orderservice.api.repository.ProductRepository;
import com.nicolafogliaro.orderservice.api.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductServiceImplConcurrencyTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    private Long productId;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        Product product = new Product();
        product.setName("Test Product");
        product.setPrice(BigDecimal.valueOf(150.00));
        product.setStockQuantity(100);
        product = productRepository.save(product);

        productId = product.getId();
    }

    @Test
    void testSuccessfulStockDecrement() {
        productService.updateStock(productId, -10);
        Product updated = productRepository.findById(productId).orElseThrow();
        assertEquals(90, updated.getStockQuantity());
    }

    @Test
    void testInsufficientStockThrowsException() {
        assertThrows(InsufficientStockException.class, () -> {
            productService.updateStock(productId, -200);
        });
    }

    @Test
    void testHighConcurrencyStockDecrement() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    productService.updateStock(productId, -1);
                    successCount.incrementAndGet();
                } catch (InsufficientStockException e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        Product product = productRepository.findById(productId).orElseThrow();
        System.out.println("Successes: " + successCount.get() + ", Failures: " + failureCount.get());
        System.out.println("Final stockQuantity: " + product.getStockQuantity());

        // Only 100 units, so max 100 successes expected
        assertEquals(100, successCount.get() + failureCount.get());
        assertEquals(100 - successCount.get(), product.getStockQuantity());
    }

}
