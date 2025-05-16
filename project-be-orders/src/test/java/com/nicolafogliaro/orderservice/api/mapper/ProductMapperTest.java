package com.nicolafogliaro.orderservice.api.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nicolafogliaro.orderservice.api.dto.product.CreateProductRequest;
import com.nicolafogliaro.orderservice.api.dto.product.ProductResponse;
import com.nicolafogliaro.orderservice.api.dto.product.UpdateProductRequest;
import com.nicolafogliaro.orderservice.api.model.Product;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ProductMapperTest {

    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUpAll() {
        objectMapper = new ObjectMapper();
        // Configure ObjectMapper as needed:
        // For consistent field order (important for string comparison!)
        objectMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS); // For Maps within object
        // objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true); // For general properties
        // If your Product class has Java 8 date/time types (LocalDate, LocalDateTime, etc.)
        objectMapper.registerModule(new JavaTimeModule());
        // Disable writing dates as timestamps if you prefer ISO-8601 strings
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Asserts that two objects are equal by comparing their JSON representations.
     *
     * @param expected      The expected object.
     * @param actual        The actual object to compare against the expected one.
     * @param objectMapper  The ObjectMapper instance to use for serialization.
     * @param messagePrefix A prefix for assertion failure messages.
     * @param <T>           The type of the objects being compared.
     * @throws JsonProcessingException if there's an error serializing objects to JSON.
     */
    public static <T> void assertObjectsEqualByJson(T expected, T actual,
                                                    ObjectMapper objectMapper, String messagePrefix) throws JsonProcessingException {

        if (expected == null && actual == null) {
            return; // Both are null, considered equal
        }
        // Handle cases where one is null and the other isn't
        if (expected == null) {
            fail(messagePrefix + ": Expected object was null, but actual was not.");
        }

        if (actual == null) {
            fail(messagePrefix + ": Actual object was null, but expected was not.");
        }

        String expectedJson = objectMapper.writeValueAsString(expected);
        String actualJson = objectMapper.writeValueAsString(actual);
        assertEquals(expectedJson, actualJson, messagePrefix + ": JSON representations do not match.");
    }

    @Test
    void toDto_whenEntityIsNull_shouldReturnNull() {
        ProductResponse dto = ProductMapper.toDto(null);
        assertNull(dto, "DTO should be null when entity is null");
    }

    @Test
    void toDto_whenEntityIsValid_shouldMapAllFields() {

        LocalDateTime now = LocalDateTime.now();

        Product entity = Product.builder()
                .id(1L)
                .name("Test Product")
                .description("This is a test product.")
                .price(new BigDecimal("99.99"))
                .stockQuantity(100)
                .createdAt(now)
                .updatedAt(now)
                .build();

        ProductResponse expectedDto = new ProductResponse(
                1L,
                "Test Product",
                "This is a test product.",
                new BigDecimal("99.99"),
                100,
                now,
                now
        );

        ProductResponse actualDto = ProductMapper.toDto(entity);

        assertNotNull(actualDto, "DTO should not be null for a valid entity");
        assertEquals(expectedDto.id(), actualDto.id());
        assertEquals(expectedDto.name(), actualDto.name());
        assertEquals(expectedDto.description(), actualDto.description());
        assertEquals(0, expectedDto.price().compareTo(actualDto.price()), "Prices should match"); // BigDecimal comparison
        assertEquals(expectedDto.stockQuantity(), actualDto.stockQuantity());

        // Alternatively, if ProductResponse has a proper equals method:
        assertEquals(expectedDto, actualDto, "Mapped DTO should match expected DTO");
    }

    @Test
    void toEntity_whenDtoIsNull_shouldReturnNull() {
        Product entity = ProductMapper.toEntity(null);
        assertNull(entity, "Entity should be null when DTO is null");
    }

    @Test
    void toEntity_whenDtoIsValid_shouldMapAllFields() {
        CreateProductRequest dto = new CreateProductRequest(
                "New Product",
                "Description for new product",
                new BigDecimal("19.95"),
                50
        );

        Product actualEntity = ProductMapper.toEntity(dto);

        assertNotNull(actualEntity, "Entity should not be null for a valid DTO");
        assertNull(actualEntity.getId(), "ID should be null for a new entity from CreateProductRequest");
        assertEquals(dto.name(), actualEntity.getName());
        assertEquals(dto.description(), actualEntity.getDescription());
        assertEquals(0, dto.price().compareTo(actualEntity.getPrice()), "Prices should match");
        assertEquals(dto.stockQuantity(), actualEntity.getStockQuantity());
    }

    @Test
    void updateEntityFromDto_whenDtoAndEntityAreNull_shouldNotThrowExceptionAndDoNothing() {
        assertDoesNotThrow(() -> ProductMapper.updateEntityFromDto(null, null),
                "Should not throw an exception when both DTO and entity are null");
    }

    @Test
    void updateEntityFromDto_whenDtoIsNull_shouldNotChangeEntity() throws JsonProcessingException {

        Product entity = Product.builder()
                .id(1L)
                .name("Original Name")
                .description("Original Description")
                .price(new BigDecimal("10.00"))
                .stockQuantity(10)
                .build();

        Product originalEntityCopy = Product.builder() // To compare against
                .id(1L)
                .name("Original Name")
                .description("Original Description")
                .price(new BigDecimal("10.00"))
                .stockQuantity(10)
                .build();

        ProductMapper.updateEntityFromDto(null, entity);

        assertEquals(originalEntityCopy.getName(), entity.getName(), "Name should not change");
        assertEquals(originalEntityCopy.getDescription(), entity.getDescription(), "Description should not change");
        assertEquals(0, originalEntityCopy.getPrice().compareTo(entity.getPrice()), "Price should not change");
        assertEquals(originalEntityCopy.getStockQuantity(), entity.getStockQuantity(), "Stock quantity should not change");
        // Using .equals if Product has a proper equals method
        assertObjectsEqualByJson(originalEntityCopy, entity, objectMapper, "Entity should remain unchanged when DTO is null");
    }

    @Test
    void updateEntityFromDto_whenEntityIsNull_shouldNotThrowExceptionAndDoNothing() {
        UpdateProductRequest dto = new UpdateProductRequest(
                "Updated Name",
                "Updated Description",
                new BigDecimal("20.00"),
                20
        );
        assertDoesNotThrow(() -> ProductMapper.updateEntityFromDto(dto, null),
                "Should not throw an exception when entity is null");
    }

    @Test
    void updateEntityFromDto_whenDtoHasAllFields_shouldUpdateAllEntityFields() {
        Product entity = Product.builder()
                .id(1L)
                .name("Old Name")
                .description("Old Description")
                .price(new BigDecimal("5.00"))
                .stockQuantity(5)
                .build();

        UpdateProductRequest dto = new UpdateProductRequest(
                "New Name",
                "New Description",
                new BigDecimal("15.50"),
                15
        );

        ProductMapper.updateEntityFromDto(dto, entity);

        assertEquals("New Name", entity.getName());
        assertEquals("New Description", entity.getDescription());
        assertEquals(0, new BigDecimal("15.50").compareTo(entity.getPrice()));
        assertEquals(15, entity.getStockQuantity());
    }

    @Test
    void updateEntityFromDto_whenDtoHasSomeNullFields_shouldUpdateOnlyNonNullFields() {
        Product entity = Product.builder()
                .id(1L)
                .name("Original Name")
                .description("Original Description")
                .price(new BigDecimal("10.00"))
                .stockQuantity(10)
                .build();

        UpdateProductRequest dtoWithNulls = new UpdateProductRequest(
                "Updated Name",       // Name will be updated
                null,                 // Description will NOT be updated
                new BigDecimal("12.25"), // Price will be updated
                null                  // Stock will NOT be updated
        );

        ProductMapper.updateEntityFromDto(dtoWithNulls, entity);

        assertEquals("Updated Name", entity.getName(), "Name should be updated");
        assertEquals("Original Description", entity.getDescription(), "Description should remain original");
        assertEquals(0, new BigDecimal("12.25").compareTo(entity.getPrice()), "Price should be updated");
        assertEquals(10, entity.getStockQuantity(), "Stock quantity should remain original");
    }

    @Test
    void updateEntityFromDto_whenAllDtoFieldsAreNull_shouldNotChangeEntity() throws JsonProcessingException {

        Product entity = Product.builder()
                .id(1L)
                .name("Original Name")
                .description("Original Description")
                .price(new BigDecimal("10.00"))
                .stockQuantity(10)
                .build();

        Product originalEntityCopy = Product.builder()
                .id(1L)
                .name("Original Name")
                .description("Original Description")
                .price(new BigDecimal("10.00"))
                .stockQuantity(10)
                .build();

        UpdateProductRequest dtoWithAllNulls = new UpdateProductRequest(null, null, null, null);

        ProductMapper.updateEntityFromDto(dtoWithAllNulls, entity);

        assertObjectsEqualByJson(originalEntityCopy, entity, objectMapper, "Entity should remain unchanged when all DTO fields are null");
    }


}

