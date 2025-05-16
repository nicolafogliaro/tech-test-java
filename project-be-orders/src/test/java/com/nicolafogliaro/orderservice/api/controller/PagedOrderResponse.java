package com.nicolafogliaro.orderservice.api.controller;


import com.nicolafogliaro.orderservice.api.dto.order.OrderResponse;
import lombok.Data;

import java.util.List;

@Data
public class PagedOrderResponse {
    private List<OrderResponse> content;
    private int number;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
