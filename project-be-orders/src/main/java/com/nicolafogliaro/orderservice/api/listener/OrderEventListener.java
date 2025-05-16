package com.nicolafogliaro.orderservice.api.listener;

import com.nicolafogliaro.orderservice.api.model.order.Order;
import com.nicolafogliaro.orderservice.api.service.MeilisearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * A listener to automatically index orders when they change
 */
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final MeilisearchService meilisearchService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreatedOrUpdated(OrderChangedEvent event) {
        meilisearchService.indexOrder(event.getOrder());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderDeleted(OrderDeletedEvent event) {
        meilisearchService.deleteOrder(event.getOrderId());
    }

    public static class OrderChangedEvent {
        private final Order order;

        public OrderChangedEvent(Order order) {
            this.order = order;
        }

        public Order getOrder() {
            return order;
        }
    }

    public static class OrderDeletedEvent {
        private final Long orderId;

        public OrderDeletedEvent(Long orderId) {
            this.orderId = orderId;
        }

        public Long getOrderId() {
            return orderId;
        }
    }

}
