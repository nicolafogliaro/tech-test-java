package com.nicolafogliaro.orderservice.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicolafogliaro.orderservice.api.dto.product.CreateProductRequest;
import com.nicolafogliaro.orderservice.api.dto.product.ProductResponse;
import com.nicolafogliaro.orderservice.api.dto.product.UpdateProductRequest;
import com.nicolafogliaro.orderservice.api.exception.ProductNotFoundException;
import com.nicolafogliaro.orderservice.api.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Make sure ProductController, ProductServiceImpl, DTOs, and ProductNotFoundException
// are accessible (e.g., defined in the same file for this example, or imported).

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductServiceImpl productService; // Mocking the concrete class used in controller

    @Autowired
    private ObjectMapper objectMapper;

    private ProductResponse sampleProductResponse1;
    private ProductResponse sampleProductResponse2;
    private CreateProductRequest sampleCreateProductRequest;
    private UpdateProductRequest sampleUpdateProductRequest;

    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {

        sampleProductResponse1 = new ProductResponse(
                1L,
                "Laptop Pro",
                "High-end laptop",
                new BigDecimal("1200.00"),
                10,
                now,
                now
        );

        sampleProductResponse2 = new ProductResponse(
                2L, "Wireless Mouse", "Ergonomic wireless mouse", new BigDecimal("25.00"), 50, now, now
        );

        sampleCreateProductRequest = new CreateProductRequest(
                "New Gadget", "A brand new gadget", new BigDecimal("99.99"), 20
        );

        sampleUpdateProductRequest = new UpdateProductRequest(
                "Updated Gadget Name", "Updated description", new BigDecimal("109.99"), 15
        );
    }

    @Test
    void getAllProducts_shouldReturnListOfProducts() throws Exception {
        List<ProductResponse> products = Arrays.asList(sampleProductResponse1, sampleProductResponse2);
        when(productService.getAllProducts()).thenReturn(products);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is(sampleProductResponse1.name())))
                .andExpect(jsonPath("$[1].name", is(sampleProductResponse2.name())));

        verify(productService).getAllProducts();
    }

    @Test
    void getAllProducts_whenNoProducts_shouldReturnEmptyList() throws Exception {
        when(productService.getAllProducts()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(productService).getAllProducts();
    }

    @Test
    void getProductById_whenProductExists_shouldReturnProduct() throws Exception {
        Long productId = 1L;
        when(productService.getProductById(productId)).thenReturn(sampleProductResponse1);

        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(productId.intValue())))
                .andExpect(jsonPath("$.name", is(sampleProductResponse1.name())));

        verify(productService).getProductById(productId);
    }

    @Test
    void getProductById_whenProductNotFound_shouldReturnNotFound() throws Exception {
        Long productId = 99L;
        when(productService.getProductById(productId)).thenThrow(new ProductNotFoundException("Product not found"));

        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isNotFound()); // Assuming @ControllerAdvice handles ProductNotFoundException

        verify(productService).getProductById(productId);
    }

    @Test
    void createProduct_withValidRequest_shouldReturnCreatedProduct() throws Exception {
        // Assuming 'sampleProductResponse1' is what createProduct would return for 'sampleCreateProductRequest'
        // For a more precise test, the response from createProduct should match its input logic.
        ProductResponse createdResponse = new ProductResponse(
                3L,
                sampleCreateProductRequest.name(),
                sampleCreateProductRequest.description(),
                sampleCreateProductRequest.price(),
                sampleCreateProductRequest.stockQuantity(),
                now,
                now);

        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(createdResponse);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateProductRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is(sampleCreateProductRequest.name())))
                .andExpect(jsonPath("$.price", is(sampleCreateProductRequest.price().doubleValue())));


        verify(productService).createProduct(eq(sampleCreateProductRequest));
    }

    // Note: Tests for invalid CreateProductRequest (400 Bad Request due to DTO validation)
    // would require @Valid on the @RequestBody parameter in the controller.
    // If such validation is handled inside the service, these tests would change.

    @Test
    void updateProduct_whenProductExistsAndValidRequest_shouldReturnUpdatedProduct() throws Exception {

        Long productId = 1L;

        ProductResponse updatedResponse = new ProductResponse(
                productId,
                sampleUpdateProductRequest.name(),
                sampleUpdateProductRequest.description(),
                sampleUpdateProductRequest.price(),
                sampleUpdateProductRequest.stockQuantity(),
                sampleProductResponse1.createdAt(),
                now);

        when(productService.updateProduct(eq(productId), any(UpdateProductRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUpdateProductRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(productId.intValue())))
                .andExpect(jsonPath("$.name", is(sampleUpdateProductRequest.name())))
                .andExpect(jsonPath("$.description", is(sampleUpdateProductRequest.description())));

        verify(productService).updateProduct(eq(productId), eq(sampleUpdateProductRequest));
    }

    @Test
    void updateProduct_whenProductNotFound_shouldReturnNotFound() throws Exception {
        Long productId = 99L;
        when(productService.updateProduct(eq(productId), any(UpdateProductRequest.class)))
                .thenThrow(new ProductNotFoundException("Product not found"));

        mockMvc.perform(put("/api/v1/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUpdateProductRequest)))
                .andExpect(status().isNotFound()); // Assuming @ControllerAdvice

        verify(productService).updateProduct(eq(productId), eq(sampleUpdateProductRequest));
    }

    @Test
    void deleteProduct_whenProductExists_shouldReturnNoContent() throws Exception {
        Long productId = 1L;
        doNothing().when(productService).deleteProduct(productId);

        mockMvc.perform(delete("/api/v1/products/{id}", productId))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(productId);
    }

    @Test
    void deleteProduct_whenProductNotFound_shouldReturnNotFound() throws Exception {
        Long productId = 99L;
        doThrow(new ProductNotFoundException("Product not found")).when(productService).deleteProduct(productId);

        mockMvc.perform(delete("/api/v1/products/{id}", productId))
                .andExpect(status().isNotFound()); // Assuming @ControllerAdvice

        verify(productService).deleteProduct(productId);
    }
}