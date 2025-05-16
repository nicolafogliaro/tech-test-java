package com.nicolafogliaro.orderservice.api.service;

import com.nicolafogliaro.orderservice.api.dto.order.OrderResponse;
import com.nicolafogliaro.orderservice.api.dto.order.OrderSearchCriteria;
import org.springframework.data.domain.Page;

public interface OrderSearchService {

    Page<OrderResponse> searchOrdersWithDb(OrderSearchCriteria criteria);

    Page<OrderResponse> searchOrdersWithSearchEngine(OrderSearchCriteria criteria);
}
