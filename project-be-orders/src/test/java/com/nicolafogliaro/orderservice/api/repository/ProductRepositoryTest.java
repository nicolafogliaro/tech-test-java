package com.nicolafogliaro.orderservice.api.repository;

import com.nicolafogliaro.orderservice.api.model.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Configures H2, Hibernate, Spring Data JPA. Disables full auto-configuration.
 */
@DataJpaTest
// Reset the application context (including in-memory DB) after each test method
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager testEntityManager; // For managing entities in tests

    @Autowired
    private ProductRepository productRepository;

    @PersistenceContext
    private EntityManager entityManager; // For more fine-grained control if needed

    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        // Initialize common test data
        product1 = Product.builder()
                .name("Laptop Pro 15")
                .description("High-performance laptop")
                .price(new BigDecimal("1200.99"))
                .stockQuantity(50)
                .build();

        product2 = Product.builder()
                .name("Wireless Mouse")
                .description("Ergonomic wireless mouse")
                .price(new BigDecimal("25.50"))
                .stockQuantity(200)
                .build();
    }

    @Test
    void testSaveProduct_Success() {
        Product productToSave = Product.builder()
                .name("Test Keyboard")
                .description("Mechanical gaming keyboard")
                .price(new BigDecimal("75.00"))
                .stockQuantity(100)
                .build();

        Product savedProduct = productRepository.save(productToSave);

        assertThat(savedProduct).isNotNull();
        assertThat(savedProduct.getId()).isNotNull().isPositive();
        assertThat(savedProduct.getName()).isEqualTo("Test Keyboard");
        assertThat(testEntityManager.find(Product.class, savedProduct.getId())).isEqualTo(savedProduct);
    }

    @Test
    void testFindById_WhenProductExists() {
        Product persistedProduct = testEntityManager.persistFlushFind(product1);

        Optional<Product> foundProductOpt = productRepository.findById(persistedProduct.getId());

        assertThat(foundProductOpt).isPresent();
        assertThat(foundProductOpt.get().getName()).isEqualTo(product1.getName());
    }

    @Test
    void testFindById_WhenProductDoesNotExist() {
        Optional<Product> foundProductOpt = productRepository.findById(999L); // Non-existent ID
        assertThat(foundProductOpt).isNotPresent();
    }

    @Test
    void testFindAllProducts_WhenMultipleProductsExist() {
        testEntityManager.persist(product1);
        testEntityManager.persist(product2);
        testEntityManager.flush();

        List<Product> products = productRepository.findAll();

        assertThat(products).hasSize(2);
        assertThat(products).extracting(Product::getName).containsExactlyInAnyOrder("Laptop Pro 15", "Wireless Mouse");
    }

    @Test
    void testFindAllProducts_WhenNoProductsExist() {
        List<Product> products = productRepository.findAll();
        assertThat(products).isEmpty();
    }

    @Test
    void testUpdateProduct() {
        Product persistedProduct = testEntityManager.persistFlushFind(product1);

        // Detach to simulate fetching and then updating
        testEntityManager.detach(persistedProduct);

        Product productToUpdate = productRepository.findById(persistedProduct.getId()).orElseThrow();
        productToUpdate.setName("Laptop Pro 15 Gen 2");
        productToUpdate.setPrice(new BigDecimal("1299.99"));
        Product updatedProduct = productRepository.saveAndFlush(productToUpdate); // Use saveAndFlush to ensure update hits DB

        assertThat(updatedProduct.getName()).isEqualTo("Laptop Pro 15 Gen 2");
        assertThat(updatedProduct.getPrice()).isEqualTo(new BigDecimal("1299.99"));

        // Verify state in persistence context after clear if necessary
        testEntityManager.clear();
        Product reFetchedProduct = testEntityManager.find(Product.class, updatedProduct.getId());
        assertThat(reFetchedProduct.getName()).isEqualTo("Laptop Pro 15 Gen 2");
    }

    @Test
    void testDeleteProduct() {
        Product persistedProduct = testEntityManager.persistFlushFind(product1);
        Long productId = persistedProduct.getId();

        productRepository.deleteById(productId);
        testEntityManager.flush(); // Ensure delete is processed

        assertThat(testEntityManager.find(Product.class, productId)).isNull();
        assertThat(productRepository.findById(productId)).isNotPresent();
    }

    @Test
    void testFindByIdWithLock_WhenProductExists() {
        Product persistedProduct = testEntityManager.persistFlushFind(product1);

        // The test itself runs in a transaction, so a lock is acquired.
        // Verifying the *type* of lock programmatically in a unit test is hard.
        // We primarily test that the method works and returns the entity.
        Optional<Product> foundProductOpt = productRepository.findByIdWithLock(persistedProduct.getId());

        assertThat(foundProductOpt).isPresent();
        assertThat(foundProductOpt.get().getId()).isEqualTo(persistedProduct.getId());
        assertThat(foundProductOpt.get().getName()).isEqualTo(persistedProduct.getName());
    }

    @Test
    void testFindByIdWithLock_WhenProductDoesNotExist() {
        Optional<Product> foundProductOpt = productRepository.findByIdWithLock(999L);
        assertThat(foundProductOpt).isNotPresent();
    }

    @Test
    void testFindByNameContainingIgnoreCase_VariousScenarios() {
        Product p1 = Product.builder().name("Apple iPhone 15").description("Newest iPhone").price(BigDecimal.valueOf(999)).stockQuantity(10).build();
        Product p2 = Product.builder().name("apple iPad Pro").description("Large tablet").price(BigDecimal.valueOf(799)).stockQuantity(5).build();
        Product p3 = Product.builder().name("Samsung Galaxy S23").description("Android flagship").price(BigDecimal.valueOf(899)).stockQuantity(8).build();
        Product p4 = Product.builder().name("Logitech Mouse Pad").description("Accessory").price(BigDecimal.valueOf(10)).stockQuantity(100).build();

        productRepository.saveAll(Arrays.asList(p1, p2, p3, p4));
        testEntityManager.flush();

        // Case 1: Partial match, case-insensitive
        List<Product> found1 = productRepository.findByNameContainingIgnoreCase("iphone");
        assertThat(found1).hasSize(1).extracting(Product::getName).containsExactly("Apple iPhone 15");

        // Case 2: Different casing
        List<Product> found2 = productRepository.findByNameContainingIgnoreCase("APPLE");
        assertThat(found2).hasSize(2).extracting(Product::getName).containsExactlyInAnyOrder("Apple iPhone 15", "apple iPad Pro");

        // Case 3: Match multiple words
        List<Product> found3 = productRepository.findByNameContainingIgnoreCase("pad");
        assertThat(found3).hasSize(2).extracting(Product::getName).containsExactlyInAnyOrder("apple iPad Pro", "Logitech Mouse Pad");


        // Case 4: No match
        List<Product> found4 = productRepository.findByNameContainingIgnoreCase("pixel");
        assertThat(found4).isEmpty();

        // Case 5: Empty string (should probably return all or none, depending on DB/JPA behavior for LIKE '%%')
        // Most databases treat LIKE '%%' as matching all non-null strings.
        List<Product> found5 = productRepository.findByNameContainingIgnoreCase("");
        assertThat(found5).hasSize(4);
    }

    @Test
    void testSaveProduct_WhenNameIsNull_ThrowsException() {
        Product productWithNullName = Product.builder()
                .description("A product without a name")
                .price(new BigDecimal("10.00"))
                .stockQuantity(5)
                .build(); // Name is null

        // Hibernate might throw an exception before flushing due to @Column(nullable=false)
        // or the database will throw it on flush.
        Exception exception = assertThrows(Exception.class, () -> {
            productRepository.saveAndFlush(productWithNullName);
            // If using only save(), then flush might be needed: testEntityManager.flush();
        });

        // The specific exception can vary (e.g., JpaSystemException wrapping PropertyValueException, or DataIntegrityViolationException)
        assertThat(exception).isInstanceOfAny(DataIntegrityViolationException.class, JpaSystemException.class);
    }

    @Test
    void testSaveProduct_WhenPriceIsNull_ThrowsException() {
        Product productWithNullPrice = Product.builder()
                .name("Priceless Item")
                .description("This item has no price")
                .stockQuantity(1)
                .build(); // Price is null

        Exception exception = assertThrows(Exception.class, () -> {
            productRepository.saveAndFlush(productWithNullPrice);
        });
        assertThat(exception).isInstanceOfAny(DataIntegrityViolationException.class, JpaSystemException.class);
    }

    @Test
    void testSaveProduct_WhenStockIsNull_ThrowsException() {
        Product productWithNullStock = Product.builder()
                .name("Stockless Item")
                .description("This item has no stock quantity defined")
                .price(new BigDecimal("99.99"))
                .build(); // stockQuantity is null

        Exception exception = assertThrows(Exception.class, () -> {
            productRepository.saveAndFlush(productWithNullStock);
        });
        assertThat(exception).isInstanceOfAny(DataIntegrityViolationException.class, JpaSystemException.class);
    }

    @Test
    void testAuditFields_OnCreate() {
        Product newProduct = Product.builder()
                .name("Audit Test Product")
                .description("Testing audit fields")
                .price(new BigDecimal("1.00"))
                .stockQuantity(1)
                .build();

        LocalDateTime beforeSave = LocalDateTime.now();
        Product savedProduct = productRepository.saveAndFlush(newProduct);
        LocalDateTime afterSave = LocalDateTime.now();

        testEntityManager.clear(); // Clear context to ensure we fetch from DB

        Product fetchedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();

        assertThat(fetchedProduct.getCreatedAt()).isNotNull();
        assertThat(fetchedProduct.getUpdatedAt()).isNotNull();
        assertThat(fetchedProduct.getCreatedAt()).isEqualTo(fetchedProduct.getUpdatedAt()); // On creation, they should be same

        // Check if timestamps are within a reasonable range of the save operation
        // Allow for slight clock differences if test runs across second boundaries
        assertThat(fetchedProduct.getCreatedAt()).isBetween(beforeSave.minusSeconds(1), afterSave.plusSeconds(1));
    }

    @Test
    void testAuditFields_OnUpdate() throws InterruptedException {
        Product product = Product.builder()
                .name("Auditable Product")
                .description("Initial version for audit.")
                .price(new BigDecimal("10.00"))
                .stockQuantity(10)
                .build();

        Product savedProduct = productRepository.saveAndFlush(product);
        testEntityManager.clear(); // Ensure we fetch a fresh copy for createdAt value

        Product productToUpdate = productRepository.findById(savedProduct.getId()).orElseThrow();
        LocalDateTime originalCreatedAt = productToUpdate.getCreatedAt();
        LocalDateTime originalUpdatedAt = productToUpdate.getUpdatedAt();

        // Ensure some time passes for updatedAt to change noticeably
        Thread.sleep(50); // Small delay; adjust if needed, though often not required if DB precision is high enough

        productToUpdate.setDescription("Updated version for audit.");
        LocalDateTime beforeUpdate = LocalDateTime.now();
        Product updatedProduct = productRepository.saveAndFlush(productToUpdate);
        LocalDateTime afterUpdate = LocalDateTime.now();

        testEntityManager.clear();
        Product reFetchedProduct = productRepository.findById(updatedProduct.getId()).orElseThrow();

        assertThat(reFetchedProduct.getCreatedAt()).isEqualTo(originalCreatedAt); // CreatedAt should not change
        assertThat(reFetchedProduct.getUpdatedAt()).isNotNull();
        assertThat(reFetchedProduct.getUpdatedAt()).isNotEqualTo(originalUpdatedAt); // UpdatedAt should have changed
        assertThat(reFetchedProduct.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
        assertThat(reFetchedProduct.getUpdatedAt()).isBetween(beforeUpdate.minusSeconds(1), afterUpdate.plusSeconds(1));
    }
}