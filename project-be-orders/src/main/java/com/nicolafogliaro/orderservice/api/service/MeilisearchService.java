package com.nicolafogliaro.orderservice.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.exceptions.MeilisearchApiException;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.json.JacksonJsonHandler;
import com.meilisearch.sdk.model.SearchResultPaginated;
import com.meilisearch.sdk.model.Searchable;
import com.meilisearch.sdk.model.Settings;
import com.meilisearch.sdk.model.TaskInfo;
import com.nicolafogliaro.orderservice.api.dto.order.OrderResponse;
import com.nicolafogliaro.orderservice.api.dto.order.OrderSearchCriteria;
import com.nicolafogliaro.orderservice.api.mapper.OrderMapper;
import com.nicolafogliaro.orderservice.api.model.order.Order;
import com.nicolafogliaro.orderservice.api.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class MeilisearchService {

    private static final String ORDERS_INDEX = "orders";

    @Value("${meilisearch.host:http://localhost:7700}")
    private String meilisearchHost;

    @Value("${meilisearch.api-key:}")
    private String meilisearchApiKey;

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    /**
     * Get a Meilisearch client
     */
    private Client getClient() {
        Config config = new Config(meilisearchHost, meilisearchApiKey, new JacksonJsonHandler(objectMapper));
        return new Client(config);
    }

    @Transactional(readOnly = true) // readOnly = true is a good practice for read operations
    public void initializeIndexes() {
        log.info(">>> [{}#initializeIndexes] ", MeilisearchService.class.getSimpleName());

        try {

            Client client = getClient();

            log.info(">>> [{}#initializeIndexes] client: {}", MeilisearchService.class.getSimpleName(), client.getIndexes().getResults());


            // Check if the index exists, create it if it doesn't
            try {
                Index index = client.index(ORDERS_INDEX);

                log.info(">>> [{}#initializeIndexes] index: {}", MeilisearchService.class.getSimpleName(), index.getSettings().getSearchableAttributes());
                syncAllOrders();

            } catch (MeilisearchApiException e) {

                log.error("*** [{}#initializeIndexes] Failed to initialize Meilisearch indexes: {}", MeilisearchService.class.getSimpleName(), e.getMessage(), e);

                if (e.getMessage().contains("Index `orders` not found")) {
                    // Create the index
                    TaskInfo id = client.createIndex(ORDERS_INDEX, "id");
                    log.info("Created new index {} with primary key 'id'", ORDERS_INDEX);

                    Index index = client.index(ORDERS_INDEX);

                    Settings settings = index.getSettings();
                    // Configure searchable attributes based on the OrderResponse fields
                    settings.setSearchableAttributes(List.of("customerId", "description", "items.productName", "items.productDescription").toArray(String[]::new));
                    settings.setSortableAttributes(List.of("id", "createdAt", "totalAmount", "status").toArray(String[]::new));

                    TaskInfo settingsTask = index.updateSettings(settings);
                    log.info("Settings update task ID: {}", settingsTask.getTaskUid());
                } else {
                    throw e; // Re-throw if it's another kind of error
                }
            }
        } catch (Exception e) {
            // Log the error but don't prevent application startup
            log.error("*** [{}#initializeIndexes] Failed to initialize Meilisearch indexes: {}", MeilisearchService.class.getSimpleName(), e.getMessage(), e);
        }
        log.info("<<< [{}#initializeIndexes] ", MeilisearchService.class.getSimpleName());
    }

    /**
     * Search for orders using Meilisearch
     */
    public Page<OrderResponse> searchOrders(OrderSearchCriteria criteria) throws MeilisearchException {

        Client client = getClient();

        SearchRequest searchRequest = SearchRequest.builder()
                .q(criteria.getQuery())
                .limit(criteria.getSize())
                .offset(criteria.getPage() * criteria.getSize())
                .build();

        // Add sorting if needed
        if (criteria.getSort() != null) {
            String sortStr = criteria.getSort();
            if (criteria.getDirection().equalsIgnoreCase("desc")) {
                sortStr = sortStr + ":desc";
            }
            searchRequest.setSort(new String[]{sortStr});
        }

        SearchResultPaginated searchResult = (SearchResultPaginated) client.index(ORDERS_INDEX).search(searchRequest);

        // Convert hits to DTOs
        List<OrderResponse> orders = new ArrayList<>();
        searchResult.getHits().forEach(hit -> {
            try {
                OrderResponse orderDTO = objectMapper.convertValue(hit, OrderResponse.class);
                orders.add(orderDTO);
            } catch (Exception e) {
                log.error("Error converting Meilisearch hit to OrderDTO", e);
            }
        });

        return new PageImpl<>(
                orders,
                PageRequest.of(criteria.getPage(), criteria.getSize()),
                searchResult.getTotalHits()
        );
    }

    /**
     * Search for order IDs using Meilisearch
     */
    public List<Long> searchOrderIds(String query) {

        try {

            Client client = getClient();

            Index index = client.index(ORDERS_INDEX);

            SearchRequest searchRequest = SearchRequest.builder()
                    .q(query)
                    .limit(1000)// Set a reasonable limit
                    .attributesToRetrieve(new String[]{"id"})
                    .build();

            Searchable searchResult = index.search(searchRequest);

            List<Long> orderIds = new ArrayList<>();

            searchResult.getHits().forEach(hit -> {
                try {
                    orderIds.add(Long.valueOf(hit.get("id").toString()));
                } catch (Exception e) {
                    log.error("Error extracting ID from Meilisearch hit", e);
                }
            });

            return orderIds;
        } catch (Exception e) {
            log.error("Error searching Meilisearch for order IDs", e);
            return List.of();
        }
    }

    /**
     * Index an order in Meilisearch
     */
    public void indexOrder(Order order) {
        try {
            Client client = getClient();
            OrderResponse orderDTO = OrderMapper.toDto(order);
            client.index(ORDERS_INDEX).addDocuments(objectMapper.writeValueAsString(List.of(orderDTO)));
        } catch (Exception e) {
            log.error("Error indexing order in Meilisearch", e);
        }
    }

    /**
     * Delete an order from Meilisearch
     */
    public void deleteOrder(Long orderId) {
        try {
            Client client = getClient();
            client.index(ORDERS_INDEX).deleteDocument(orderId.toString());
        } catch (Exception e) {
            log.error("Error deleting order from Meilisearch", e);
        }
    }

    /**
     * Delete an order from Meilisearch
     */
    public void deleteOrder2(Long orderId) {
        try {
            Client client = getClient();
            TaskInfo task = client.index(ORDERS_INDEX).deleteDocument(orderId.toString());
            log.debug("Order deleted from Meilisearch with task ID: {}", task.getTaskUid());
        } catch (Exception e) {
            log.error("Error deleting order from Meilisearch", e);
        }
    }

    /**
     * Scheduled job to sync all orders with Meilisearch
     */
    @Scheduled(cron = "0 0 1 * * ?") // Daily at 1 AM
    public void syncAllOrders() {

        log.info("Starting scheduled Meilisearch sync");

        try {

            List<Order> allOrders = orderRepository.findAll();

            List<OrderResponse> orderDTOs = allOrders.stream()
                    .map(OrderMapper::toDto)
                    .collect(Collectors.toList());

            Client client = getClient();

            Index index = client.index(ORDERS_INDEX);

            // Reset the index - try to delete all documents first
            try {
                TaskInfo deleteTask = index.deleteAllDocuments();
                log.debug("Delete all documents task ID: {}", deleteTask.getTaskUid());
            } catch (Exception e) {
                log.warn("Error deleting documents from Meilisearch index", e);

                // If deleting documents fails, try to delete and recreate the index
                try {
                    client.deleteIndex(ORDERS_INDEX);
                    client.createIndex(ORDERS_INDEX, "id");
                    log.info("Created new index {} with primary key 'id'", ORDERS_INDEX);
                } catch (Exception ex) {
                    log.warn("Error recreating Meilisearch index", ex);
                }
            }

            Settings settings = index.getSettings();

            // Configure searchable attributes based on the OrderResponse fields
            settings.setSearchableAttributes(List.of("customerId", "description", "items.productName", "items.productDescription").toArray(String[]::new));
            settings.setSortableAttributes(List.of("id", "createdAt", "totalAmount", "status").toArray(String[]::new));

            TaskInfo settingsTask = index.updateSettings(settings);
            log.debug("Settings update task ID: {}", settingsTask.getTaskUid());

            // Batch index documents
            if (!orderDTOs.isEmpty()) {

                TaskInfo indexTask = index
                        .addDocuments(objectMapper.writeValueAsString(orderDTOs));

                log.debug("Sync indexing task ID: {}", indexTask.getTaskUid());
            }

            log.info("Completed Meilisearch sync, indexed {} orders", orderDTOs.size());
        } catch (Exception e) {
            log.error("Error during Meilisearch sync", e);
        }
    }

}
