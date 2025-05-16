package com.nicolafogliaro.orderservice.api.controller;


import com.nicolafogliaro.orderservice.api.dto.order.OrderRequest;
import com.nicolafogliaro.orderservice.api.dto.order.OrderResponse;
import com.nicolafogliaro.orderservice.api.dto.order.OrderSearchCriteria;
import com.nicolafogliaro.orderservice.api.dto.order.UpdateOrderRequest;
import com.nicolafogliaro.orderservice.api.service.OrderSearchService;
import com.nicolafogliaro.orderservice.api.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

/**
 * http://localhost:8080/order-service/api/v1/orders
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order API", description = "Endpoints for managing orders, including creation, retrieval, updating, deletion, searching")
public class OrderController {

    private final OrderService orderService;
    private final OrderSearchService orderSearchService;


    /**
     * Get a specific order by its ID, including associated product details.
     */
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public OrderResponse getOrderById(@PathVariable Long id) {
        log.info(">>> [{}#getOrderById] --> id: {}", OrderController.class.getSimpleName(), id);
        OrderResponse res = orderService.getOrderById(id);
        log.info("<<< [{}#getOrderById] ---> id: {} | <--- res: {}", OrderController.class.getSimpleName(), id, res);
        return res;
    }

    /**
     * Create a new order.
     * Stock levels for the ordered products will be checked and updated by the OrderService.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody OrderRequest request) {
        log.info(">>> [{}#createOrder] --> req: {}", OrderController.class.getSimpleName(), request);
        OrderResponse res = orderService.createOrder(request);
        log.info("<<< [{}#createOrder] --> req: {} | <--- res: {}", OrderController.class.getSimpleName(), request, res);
        return res;
    }

    /**
     * Update an existing order.
     * Stock levels will be adjusted by the OrderService based on changes in the order.
     */
    @PutMapping("/{id}")
    public OrderResponse updateOrder(@PathVariable Long id,
                                     @Valid @RequestBody UpdateOrderRequest request) {
        log.info(">>> [{}#updateOrder] --> id: {}, req: {}", OrderController.class.getSimpleName(), id, request);
        OrderResponse res = orderService.updateOrder(id, request);
        log.info("<<< [{}#updateOrder] --> id: {}, req: {} | <--- res: {}", OrderController.class.getSimpleName(), id, request, res);
        return res;

    }

    /**
     * Delete an order.
     * The OrderService will handle any necessary stockQuantity adjustments (e.g., returning items to stockQuantity).
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(@PathVariable Long id) {
        log.info(">>> [{}#deleteOrder] --> id: {}", OrderController.class.getSimpleName(), id);
        orderService.deleteOrder(id);
        log.info("<<< [{}#deleteOrder] --> id: {}", OrderController.class.getSimpleName(), id);
    }


    /**
     * Search Implementation:
     * <p>
     * Database Search: Using Spring Data JPA Specifications for flexible querying
     * Meilisearch Integration: For high-performance text search capabilities
     * Fallback Mechanism: If Meilisearch fails, the system defaults to database search
     * <p>
     * Search Features:
     * <p>
     * Text search across order name and description
     * Date range filtering
     * Pagination and sorting options
     * Combined search using both Meilisearch and database when needed
     * <p>
     * Automatic Indexing:
     * <p>
     * Event listeners automatically update the Meilisearch index when orders change
     * Scheduled daily sync ensures consistency between database and search index
     * <p>
     * Search API Endpoints:
     * <p>
     * Primary endpoint at /api/orders/search using Meilisearch
     * Backup endpoint at /api/orders/search/db using only database search
     *
     * @param request
     * @return
     */
    @PostMapping(path = "/search", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Page<OrderResponse> searchOrders(@Valid @RequestBody OrderSearchCriteria request) {
        log.info(">>> [{}#searchOrders] --> req: {}", OrderController.class.getSimpleName(), request);
        Page<OrderResponse> orderResponses = orderSearchService.searchOrdersWithDb(request);
        log.info("<<< [{}#searchOrders] --> req: {}", OrderController.class.getSimpleName(), request);
        return orderResponses;
    }

    @PostMapping(path = "/search/engine", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Page<OrderResponse> searchOrdersWithSearchEngine(@Valid @RequestBody OrderSearchCriteria request) {
        log.info(">>> [{}#searchOrdersWithSearchEngine] --> req: {}", OrderController.class.getSimpleName(), request);
        return orderSearchService.searchOrdersWithSearchEngine(request);
    }

}
