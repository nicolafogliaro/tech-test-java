package com.nicolafogliaro.orderservice.api.controller;


import lombok.Data;

import java.util.List;

@Data
public class PagedResponse<T> {
    private List<T> content;
    private int number;
    private int size;
    private int totalPages;
    private long totalElements;
    private boolean last;
}
