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
import java.util.Arrays;
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
        log.info(">>> [{}#initializeIndexes] Attempting to initialize Meilisearch indexes...", MeilisearchService.class.getSimpleName());

        try {
            Client client = getClient();
            if (client == null) {
                log.error("*** [{}#initializeIndexes] Meilisearch client is null. Cannot initialize indexes.", MeilisearchService.class.getSimpleName());
                return;
            }

            log.info(">>> [{}#initializeIndexes] Current Meilisearch indexes: {}", MeilisearchService.class.getSimpleName(), client.getIndexes().getResults());

            // Check if the 'orders' index exists and is configured
            try {
                Index index = client.index(ORDERS_INDEX);
                Settings currentSettings = index.getSettings(); // This will throw MeilisearchApiException if index is not found
                log.info("Index {} already exists. Searchable attributes: {}. Proceeding to sync orders.",
                        ORDERS_INDEX, Arrays.toString(currentSettings.getSearchableAttributes()));
                syncAllOrders();
            } catch (MeilisearchApiException e) {
                // Check if the exception is specifically "index_not_found"
                // Based on logs, MeilisearchApiException has e.getCode() for "index_not_found"
                // and e.getType() for "invalid_request"
                if ("index_not_found".equals(e.getCode())) {
                    log.warn("Index {} not found (Code: {}, Type: {}, Message: {}). Attempting to create and configure it.",
                            ORDERS_INDEX, e.getCode(), e.getType(), e.getMessage());
                    try {
                        createAndConfigureIndexInternal(client, ORDERS_INDEX);
                        log.info("Index {} created and configured successfully. Proceeding with data synchronization.", ORDERS_INDEX);
                        syncAllOrders(); // Sync after successful creation and configuration
                    } catch (MeilisearchException creationEx) {
                        // Catch exceptions specifically from the creation/configuration process
                        log.error("*** [{}#initializeIndexes] Critical error during creation/configuration of index {}: {}. This might affect application functionality.",
                                MeilisearchService.class.getSimpleName(), ORDERS_INDEX, creationEx.getMessage(), creationEx);
                        // Propagate this to the outer catch which logs but allows startup
                        throw creationEx;
                    }
                } else {
                    // If it's a different Meilisearch API error (e.g., authentication, malformed request)
                    log.error("*** [{}#initializeIndexes] Unhandled MeilisearchApiException while checking index {}. Code: {}, Type: {}, Link: {}, Message: {}. Re-throwing to indicate a critical issue.",
                            MeilisearchService.class.getSimpleName(), ORDERS_INDEX, e.getCode(), e.getType(), e.getLink(), e.getMessage(), e);
                    throw e; // Re-throw to make startup fail clearly if it's not "index_not_found" that we can handle by creation
                }
            }
        } catch (Exception e) {
            // Catch-all for other issues like getClient() failing, or re-thrown exceptions.
            // Log the error but don't prevent application startup, as per original logic.
            log.error("*** [{}#initializeIndexes] Failed to initialize Meilisearch indexes due to an unexpected error: {}. Application will continue, but Meilisearch functionality may be impaired.",
                    MeilisearchService.class.getSimpleName(), e.getMessage(), e);
        }
        log.info("<<< [{}#initializeIndexes] Meilisearch index initialization process completed.", MeilisearchService.class.getSimpleName());
    }

    /**
     * Creates the specified index, waits for its creation, then configures its settings and waits for that.
     *
     * @param client   The Meilisearch client.
     * @param indexUid The UID of the index to create and configure.
     * @throws MeilisearchException if any Meilisearch operation fails (e.g., timeout, communication error).
     */
    private void createAndConfigureIndexInternal(Client client, String indexUid) throws MeilisearchException {
        log.info(">>> [{}#createAndConfigureIndexInternal] Creating index '{}' with primary key 'id'.", MeilisearchService.class.getSimpleName(), indexUid);

        // 1. Create the index
        TaskInfo creationTaskInfo = client.createIndex(indexUid, "id");
        log.info("Index creation task enqueued for '{}'. Task UID: {}. Waiting for completion...", indexUid, creationTaskInfo.getTaskUid());
        client.waitForTask(creationTaskInfo.getTaskUid()); // Waits with default timeout and interval
        log.info("Index '{}' created successfully.", indexUid);

        // 2. Configure settings for the newly created index
        Index index = client.index(indexUid); // Get a reference to the now-existing index
        Settings settings = new Settings();

        // Define searchable and sortable attributes
        List<String> searchableAttributesList = List.of("customerId", "description", "items.productName", "items.productDescription");
        List<String> sortableAttributesList = List.of("id", "createdAt", "totalAmount", "status");

        settings.setSearchableAttributes(searchableAttributesList.toArray(String[]::new));
        settings.setSortableAttributes(sortableAttributesList.toArray(String[]::new));

        // You can configure other settings like filterableAttributes, rankingRules, etc. here
        // settings.setFilterableAttributes(List.of("status", "customerId").toArray(String[]::new));

        log.info("Updating settings for index '{}'. Searchable: {}, Sortable: {}. Waiting for task completion...",
                indexUid,
                searchableAttributesList,
                sortableAttributesList);

        TaskInfo settingsUpdateTaskInfo = index.updateSettings(settings);
        log.info("Settings update task enqueued for index '{}'. Task UID: {}", indexUid, settingsUpdateTaskInfo.getTaskUid());
        client.waitForTask(settingsUpdateTaskInfo.getTaskUid()); // Wait for settings update to complete

        log.info("Settings for index '{}' updated successfully.", indexUid);
        log.info("<<< [{}#createAndConfigureIndexInternal] Index '{}' fully created and configured.", MeilisearchService.class.getSimpleName(), indexUid);
    }

    /**
     * Search for orders using Meilisearch
     */
    public Page<OrderResponse> searchOrders(OrderSearchCriteria criteria) throws MeilisearchException {

        Client client = getClient();

        SearchRequest searchRequest = SearchRequest.builder().q(criteria.getQuery()).limit(criteria.getSize()).offset(criteria.getPage() * criteria.getSize()).build();

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

        return new PageImpl<>(orders, PageRequest.of(criteria.getPage(), criteria.getSize()), searchResult.getTotalHits());
    }

    /**
     * Search for order IDs using Meilisearch
     */
    public List<Long> searchOrderIds(String query) {

        try {

            Client client = getClient();

            Index index = client.index(ORDERS_INDEX);

            SearchRequest searchRequest = SearchRequest.builder().q(query).limit(1000)// Set a reasonable limit
                    .attributesToRetrieve(new String[]{"id"}).build();

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

            List<OrderResponse> orderDTOs = allOrders.stream().map(OrderMapper::toDto).collect(Collectors.toList());

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

                TaskInfo indexTask = index.addDocuments(objectMapper.writeValueAsString(orderDTOs));

                log.debug("Sync indexing task ID: {}", indexTask.getTaskUid());
            }

            log.info("Completed Meilisearch sync, indexed {} orders", orderDTOs.size());
        } catch (Exception e) {
            log.error("Error during Meilisearch sync", e);
        }
    }

}
