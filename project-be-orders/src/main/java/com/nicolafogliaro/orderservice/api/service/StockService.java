package com.nicolafogliaro.orderservice.api.service;

public interface StockService {

    void updateStock(Long productId, int quantityChange);

    boolean checkStockAvailability(Long productId, int requestedQuantity);
}
