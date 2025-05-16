package com.nicolafogliaro.orderservice.api.model.order;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents the possible states of an order within the system lifecycle.
 * Statuses are ordered roughly according to the typical order processing flow.
 */
@Schema(description = "Status values representing the state of an order")
public enum OrderStatus {
    /**
     * Initial state when order is first created in the system
     */
    CREATED,

    /**
     * Order has been placed but awaiting confirmation or payment
     */
    PENDING,

    /**
     * Order has been confirmed and payment processed successfully
     */
    CONFIRMED,

    /**
     * Order is being prepared or assembled for shipping
     */
    PROCESSING,

    /**
     * Order has been dispatched and is in transit
     */
    SHIPPED,

    /**
     * Order has been successfully delivered to the customer
     */
    DELIVERED,

    /**
     * Order has been completed successfully including delivery
     * and any post-delivery processes
     */
    COMPLETED,

    /**
     * Order was canceled by customer or system before completion
     */
    CANCELED,

    /**
     * Order was returned by customer after delivery
     */
    RETURNED,

    /**
     * Payment for the order has been refunded
     */
    REFUNDED,

    /**
     * Order processing could not be completed due to an error
     */
    FAILED
}

