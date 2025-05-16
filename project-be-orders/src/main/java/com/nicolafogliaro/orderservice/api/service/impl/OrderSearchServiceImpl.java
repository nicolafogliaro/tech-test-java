package com.nicolafogliaro.orderservice.api.service.impl;

import com.nicolafogliaro.orderservice.api.dto.order.OrderResponse;
import com.nicolafogliaro.orderservice.api.dto.order.OrderSearchCriteria;
import com.nicolafogliaro.orderservice.api.mapper.OrderMapper;
import com.nicolafogliaro.orderservice.api.model.order.Order;
import com.nicolafogliaro.orderservice.api.repository.OrderRepository;
import com.nicolafogliaro.orderservice.api.repository.specification.OrderSpecifications;
import com.nicolafogliaro.orderservice.api.service.MeilisearchService;
import com.nicolafogliaro.orderservice.api.service.OrderSearchService;
import com.nicolafogliaro.orderservice.api.util.MyTextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.nicolafogliaro.orderservice.api.model.order.OrderColumnNameForSearch.CREATED_AT;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class OrderSearchServiceImpl implements OrderSearchService {

    private final OrderRepository orderRepository;

    private final MeilisearchService meilisearchService;

    /**
     * Search orders using database query with specifications
     */
    @Override
    public Page<OrderResponse> searchOrdersWithDb(OrderSearchCriteria criteria) {

        log.info("Searching orders with criteria: {}", criteria);

        String sortField = MyTextUtils.nonEmpty(criteria.getSort()) ? criteria.getSort() : CREATED_AT;

        Sort.Direction direction = MyTextUtils.nonEmpty(criteria.getDirection()) ?
                Sort.Direction.fromString(criteria.getDirection()) : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(
                criteria.getPage() != null ? criteria.getPage() : 0,
                criteria.getSize() != null ? criteria.getSize() : 20,
                Sort.by(direction, sortField)
        );

        Page<Order> ordersPage = orderRepository.findAll(
                OrderSpecifications.withSearchCriteria(criteria), pageable
        );

        Page<OrderResponse> res = ordersPage.map(OrderMapper::toDto);
        log.info("Searching orders with criteria: {} | <--- res: {}", criteria, res);
        return res;
    }


    /**
     * Search orders using Meilisearch for better text search capabilities
     */
    @Override
    public Page<OrderResponse> searchOrdersWithSearchEngine(OrderSearchCriteria criteria) {
        try {
            // First check if we need to filter by date range
            if (criteria.getStartDate() != null || criteria.getEndDate() != null) {
                // For date filtering, we need to combine Meilisearch and DB results
                return searchWithMeilisearchAndDb(criteria);
            } else {
                // For pure text search, use only Meilisearch
                return meilisearchService.searchOrders(criteria);
            }
        } catch (Exception e) {
            log.error("Meilisearch error, falling back to database search", e);
            // Fallback to database search if Meilisearch fails
            return searchOrdersWithDb(criteria);
        }
    }

    /**
     * Combined search using Meilisearch for text and database for filtering
     */
    private Page<OrderResponse> searchWithMeilisearchAndDb(OrderSearchCriteria criteria) {
        // Get IDs from Meilisearch based on text search
        List<Long> orderIds = meilisearchService.searchOrderIds(criteria.getQuery());

        if (orderIds.isEmpty()) {
            return Page.empty();
        }

        // Then filter those IDs by date using DB
        Sort sort = Sort.by(
                Sort.Direction.fromString(criteria.getDirection()),
                criteria.getSort()
        );

        Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getSize(), sort);

        Specification<Order> idSpec = (root, query, cb) -> root.get("id").in(orderIds);
        Specification<Order> dateSpec = OrderSpecifications.withSearchCriteria(criteria);

        Page<Order> filteredOrders = orderRepository.findAll(
                Specification.where(idSpec).and(dateSpec),
                pageable
        );

        return filteredOrders.map(OrderMapper::toDto);
    }
}