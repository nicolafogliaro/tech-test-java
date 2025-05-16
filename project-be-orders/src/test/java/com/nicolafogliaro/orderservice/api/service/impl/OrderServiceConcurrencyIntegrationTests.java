package com.nicolafogliaro.orderservice.api.service.impl;


import com.nicolafogliaro.orderservice.api.dto.orderitem.OrderItemRequest;
import com.nicolafogliaro.orderservice.api.dto.order.OrderRequest;
import com.nicolafogliaro.orderservice.api.dto.order.OrderResponse;
import com.nicolafogliaro.orderservice.api.exception.InsufficientStockException;
import com.nicolafogliaro.orderservice.api.model.Product;
import com.nicolafogliaro.orderservice.api.model.order.OrderStatus;
import com.nicolafogliaro.orderservice.api.repository.ProductRepository;
import com.nicolafogliaro.orderservice.api.repository.OrderRepository;
import com.nicolafogliaro.orderservice.api.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.ConcurrencyFailureException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest // Loads full application context
public class OrderServiceConcurrencyIntegrationTests {

    @Autowired
    private OrderService orderServiceUnderTest; // Your OrderService with createOrder method

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {

        // Clean up database before each test for isolation
        orderRepository.deleteAll();
        productRepository.deleteAll();

        // Setup a product
        sampleProduct = new Product(); // Use your Product constructor or setters
        sampleProduct.setName("Testable Widget");
        sampleProduct.setPrice(new BigDecimal("10.00"));
        sampleProduct.setStockQuantity(1); // Start with limited stockQuantity for some tests
        sampleProduct = productRepository.save(sampleProduct);
        log.info("Set up product ID: {}, Stock: {}", sampleProduct.getId(), sampleProduct.getStockQuantity());
    }

    @Test
    void testConcurrentOrderCreation_forSameProduct_limitedStock() throws InterruptedException {

        int initialStock = 1;

        productRepository.findById(sampleProduct.getId()).ifPresent(p -> {
            p.setStockQuantity(initialStock);
            productRepository.save(p); // Ensure stockQuantity is 1
            log.info("Product {} stockQuantity reset to {} for test", p.getId(), initialStock);
        });

        final Long productId = sampleProduct.getId();

        final int numberOfThreads = 2; // Two users trying to buy the last item

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads); // To start threads roughly together
        CountDownLatch completedLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successfulOrders = new AtomicInteger(0);
        AtomicInteger insufficientStockFailures = new AtomicInteger(0);
        AtomicInteger concurrencyFailures = new AtomicInteger(0);
        List<Throwable> exceptions = new CopyOnWriteArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {

            final int threadNum = i + 1;

            executorService.submit(() -> {
                try {
                    readyLatch.countDown(); // Signal readiness
                    readyLatch.await();     // Wait for all threads to be ready

                    log.info("Thread {} attempting to create order for product ID {}", threadNum, productId);

                    OrderItemRequest itemRequest = new OrderItemRequest(productId, 1); // Order 1 unit

                    OrderRequest orderRequest = new OrderRequest(
                            1L, // dummy customerId
                            "Thread " + threadNum + " order",
                            OrderStatus.CONFIRMED,
                            Collections.singletonList(itemRequest)
                    );

                    // This call is to the method annotated with @Transactional(isolation = Isolation.REPEATABLE_READ)
                    OrderResponse response = orderServiceUnderTest.createOrder(orderRequest);

                    successfulOrders.incrementAndGet();

                    log.info("Thread {} successfully created order ID: {}", threadNum, response.id());

                } catch (InsufficientStockException e) {
                    insufficientStockFailures.incrementAndGet();
                    exceptions.add(e);
                    log.warn("Thread {} failed with InsufficientStockException: {}", threadNum, e.getMessage());
                } catch (ConcurrencyFailureException e) { // Your custom exception for optimistic locking
                    concurrencyFailures.incrementAndGet();
                    exceptions.add(e);
                    log.warn("Thread {} failed with ConcurrencyConflictException: {}", threadNum, e.getMessage());
                } catch (Exception e) {
                    exceptions.add(e);
                    log.error("Thread {} failed with unexpected exception: {}", threadNum, e.getMessage(), e);
                } finally {
                    completedLatch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        assertTrue(completedLatch.await(15, TimeUnit.SECONDS), "Threads did not complete in time");
        executorService.shutdown();

        log.info("Successful orders: {}, Insufficient stockQuantity: {}, Concurrency failures: {}",
                successfulOrders.get(),
                insufficientStockFailures.get(),
                concurrencyFailures.get());

        exceptions.forEach(e -> log.error("Captured exception: ", e));

        // Assertions
        assertEquals(1, successfulOrders.get(), "Exactly one order should succeed for the single stockQuantity item.");

        // One of these should be 1, depending on how ProductService.decrementStock and optimistic locking are set up.
        // If optimistic locking is effective, ConcurrencyConflictException is expected.
        // If the check for stockQuantity happens after another thread already took it (and committed), InsufficientStockException.
        assertTrue(insufficientStockFailures.get() == 1 || concurrencyFailures.get() == 1,
                "Exactly one order should fail due to stockQuantity or concurrency.");

        assertEquals(numberOfThreads, successfulOrders.get() + insufficientStockFailures.get() + concurrencyFailures.get(),
                "Total outcomes should match number of threads");

        // Verify final product stockQuantity
        Product finalProductState = productRepository.findById(productId).orElseThrow();
        assertEquals(0, finalProductState.getStockQuantity(), "Product stockQuantity should be zero after successful order.");

        // Verify number of orders in DB
        long ordersInDb = orderRepository.count();
        assertEquals(1, ordersInDb, "Exactly one order should be persisted in the database.");
    }
}
