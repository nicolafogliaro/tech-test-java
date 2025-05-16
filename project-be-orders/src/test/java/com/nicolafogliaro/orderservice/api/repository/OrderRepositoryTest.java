package com.nicolafogliaro.orderservice.api.repository;


import com.nicolafogliaro.orderservice.api.model.OrderItem;
import com.nicolafogliaro.orderservice.api.model.Product;
import com.nicolafogliaro.orderservice.api.model.order.Order;
import com.nicolafogliaro.orderservice.api.model.order.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Configures H2, Hibernate, Spring Data JPA. Disables full auto-configuration.
 */
@DataJpaTest
// Reset the application context (including in-memory DB) after each test method
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class OrderRepositoryTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private OrderRepository orderRepository;

    private Product sampleProduct1;
    private Product sampleProduct2;

    @BeforeEach
    void setUp() {
        // Persist some products to be used in tests, now including 'stock'
        sampleProduct1 = Product.builder()
                .name("Test Product 1")
                .description("Description for Test Product 1")
                .price(new BigDecimal("50.00"))
                .stockQuantity(100) // Mandatory field
                .build();
        // When sampleProduct1 is persisted, its @Version field is initialized by Hibernate
        testEntityManager.persist(sampleProduct1);

        sampleProduct2 = Product.builder()
                .name("Test Product 2")
                .description("Description for Test Product 2")
                .price(new BigDecimal("150.00"))
                .stockQuantity(50) // Mandatory field
                .build();
        // When sampleProduct2 is persisted, its @Version field is initialized by Hibernate
        testEntityManager.persist(sampleProduct2);

        testEntityManager.flush(); // Ensure products are in DB with generated IDs, versions, timestamps
    }


    @Test
    public void whenFindById_thenReturnOrder() {
        Order order = Order.builder()
                .customerId(2L)
                .status(OrderStatus.PROCESSING)
                .totalAmount(new BigDecimal("75.25"))
                .build();
        testEntityManager.persist(order);
        testEntityManager.flush();

        Optional<Order> foundOrderOpt = orderRepository.findById(order.getId());

        assertThat(foundOrderOpt).isPresent();
        Order foundOrder = foundOrderOpt.get();
        assertThat(foundOrder.getCustomerId()).isEqualTo(2L);
        assertThat(foundOrder.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    public void whenSaveOrderWithOrderItems_thenOrderItemsArePersisted() {
        Order order = Order.builder()
                .customerId(3L)
                .status(OrderStatus.SHIPPED)
                .totalAmount(new BigDecimal("250.00")) // This would ideally be calculated
                .build();

        OrderItem item1 = OrderItem.builder()
                .product(sampleProduct1) // Use the persisted Product object
                .quantity(2)
                .unitPrice(sampleProduct1.getPrice()) // Price at the time of order
                // totalPrice will be calculated by @PrePersist/@PreUpdate in OrderItem
                .build();

        OrderItem item2 = OrderItem.builder()
                .product(sampleProduct2) // Use the persisted Product object
                .quantity(1)
                .unitPrice(sampleProduct2.getPrice())
                .build();

        order.addOrderItem(item1);
        order.addOrderItem(item2);

        Order savedOrder = orderRepository.save(order);
        testEntityManager.flush();
        testEntityManager.clear();

        Order fetchedOrder = orderRepository.findById(savedOrder.getId()).orElse(null);
        assertThat(fetchedOrder).isNotNull();
        assertThat(fetchedOrder.getOrderItems()).hasSize(2);

        OrderItem fetchedItem1 = fetchedOrder.getOrderItems().stream()
                .filter(oi -> oi.getProduct().getId().equals(sampleProduct1.getId())).findFirst().orElse(null);
        assertThat(fetchedItem1).isNotNull();
        assertThat(fetchedItem1.getQuantity()).isEqualTo(2);
        assertThat(fetchedItem1.getUnitPrice().compareTo(new BigDecimal("50.00"))).isEqualTo(0);
        assertThat(fetchedItem1.getOrder().getId()).isEqualTo(fetchedOrder.getId());


        OrderItem fetchedItem2 = fetchedOrder.getOrderItems().stream()
                .filter(oi -> oi.getProduct().getId().equals(sampleProduct2.getId())).findFirst().orElse(null);
        assertThat(fetchedItem2).isNotNull();
        assertThat(fetchedItem2.getQuantity()).isEqualTo(1);
        assertThat(fetchedItem2.getUnitPrice().compareTo(new BigDecimal("150.00"))).isEqualTo(0);
    }

    @Test
    public void testNotNullConstraints_status_forOrder() {
        Order order = Order.builder()
                .customerId(9L)
                .totalAmount(new BigDecimal("50.00"))
                .status(null) // status is null, should violate constraint
                .build();

        // Expecting DataIntegrityViolationException if a non-null constraint is violated at DB level
        assertThrows(DataIntegrityViolationException.class, () -> {
            orderRepository.saveAndFlush(order); // saveAndFlush will attempt to persist and flush immediately
        });
    }

    @Test
    public void testNotNullConstraints_product_forOrderItem() {
        Order order = Order.builder().customerId(10L).status(OrderStatus.CREATED).totalAmount(BigDecimal.TEN).build();
        // Order must be persisted first if OrderItem has a foreign key to Order
        testEntityManager.persistAndFlush(order);


        OrderItem item = OrderItem.builder()
                .product(null) // product is null, should violate constraint if product_id is NOT NULL
                .quantity(1)
                .unitPrice(BigDecimal.ONE)
                // .order(order) // Set the order association
                .build();

        // If OrderItem is part of Order's collection with CascadeType.ALL, saving order would cascade
        // Here we attempt to persist OrderItem directly or as part of an order
        // We'll add it to the order and save the order
        order.addOrderItem(item);


        assertThrows(DataIntegrityViolationException.class, () -> {
            // Saving the order which cascades to the invalid OrderItem
            orderRepository.saveAndFlush(order);
            // Or, if persisting item directly (and assuming order_id is nullable or set correctly but product_id isn't):
            // testEntityManager.persist(item);
            // testEntityManager.flush();
        });
    }

    @Test
    public void testRemoveOrderItem_orphanRemoval() {
        Order order = Order.builder()
                .customerId(11L)
                .status(OrderStatus.CREATED) // Using a valid status
                .totalAmount(new BigDecimal("500.00"))
                .build();

        OrderItem item1 = OrderItem.builder().product(sampleProduct1).quantity(1).unitPrice(sampleProduct1.getPrice()).build();
        OrderItem item2 = OrderItem.builder().product(sampleProduct2).quantity(1).unitPrice(sampleProduct2.getPrice()).build();

        order.addOrderItem(item1);
        order.addOrderItem(item2);

        Order savedOrder = testEntityManager.persistAndFlush(order);
        // Get IDs after persist, as they are generated
        Long item1Id = savedOrder.getOrderItems().stream().filter(oi -> oi.getProduct().getId().equals(sampleProduct1.getId())).findFirst().get().getId();
        Long item2Id = savedOrder.getOrderItems().stream().filter(oi -> oi.getProduct().getId().equals(sampleProduct2.getId())).findFirst().get().getId();

        assertThat(testEntityManager.find(OrderItem.class, item1Id)).isNotNull();
        assertThat(testEntityManager.find(OrderItem.class, item2Id)).isNotNull();

        OrderItem itemToRemove = savedOrder.getOrderItems().stream()
                .filter(oi -> oi.getId().equals(item1Id))
                .findFirst().orElseThrow();
        savedOrder.removeOrderItem(itemToRemove); // item1 is now an orphan if orphanRemoval=true

        Order updatedOrder = orderRepository.saveAndFlush(savedOrder);
        testEntityManager.clear(); // Clear context to ensure fresh load

        Order reFetchedOrder = orderRepository.findById(updatedOrder.getId()).get();
        assertThat(reFetchedOrder.getOrderItems()).hasSize(1);
        assertThat(reFetchedOrder.getOrderItems().get(0).getProduct().getId()).isEqualTo(sampleProduct2.getId());

        // Verify item1 was removed from DB due to orphanRemoval
        assertThat(testEntityManager.find(OrderItem.class, item1Id)).isNull();
        assertThat(testEntityManager.find(OrderItem.class, item2Id)).isNotNull();
    }

    @Test
    public void testOrderDeletion() {
        Order order = Order.builder()
                .customerId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .build();

        Order persistedOrder = testEntityManager.persistAndFlush(order);

        // Delete order using TestEntityManager or Repository
        // Using repository here for consistency with other tests
        orderRepository.deleteById(persistedOrder.getId());
        testEntityManager.flush(); // Ensure delete is propagated
        testEntityManager.clear(); // Clear context

        assertThat(orderRepository.findById(persistedOrder.getId())).isNotPresent();
        // Also verify with TestEntityManager
        assertThat(testEntityManager.find(Order.class, persistedOrder.getId())).isNull();
    }

    @Test
    public void testConstraintViolations() { // This test name is a bit generic, consider renaming if it tests a specific constraint
        assertThrows(DataIntegrityViolationException.class, () -> {
            Order order = Order.builder()
                    .customerId(1L)
                    .status(null) // Missing required status
                    .totalAmount(new BigDecimal("100.00"))
                    .build();
            // testEntityManager.persist(order);
            // testEntityManager.flush();
            // Using repository saveAndFlush to immediately trigger potential violations
            orderRepository.saveAndFlush(order);
        });
    }

    @Test
    public void testAuditFields() {
        Order order = Order.builder()
                .customerId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .description("Order for audit test")
                .build();

        // Persist using repository to ensure @PrePersist / @PreUpdate listeners are triggered
        // if they are part of Spring Data JPA lifecycle and not just JPA.
        // TestEntityManager also triggers JPA lifecycle callbacks.
        Order persistedOrder = orderRepository.saveAndFlush(order);
        Long orderId = persistedOrder.getId();

        testEntityManager.clear(); // Clear context to ensure fresh load from DB

        Order foundOrder = orderRepository.findById(orderId).orElse(null);
        assertThat(foundOrder).isNotNull();
        assertThat(foundOrder.getCreatedAt()).isNotNull();
        assertThat(foundOrder.getUpdatedAt()).isNotNull();
        assertThat(foundOrder.getUpdatedAt()).isEqualToIgnoringNanos(foundOrder.getCreatedAt());
    }
}
