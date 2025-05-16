package com.nicolafogliaro.orderservice.api.model.order;

import com.nicolafogliaro.orderservice.api.model.OrderItem;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The main entity with fields like name, description
 */
@Slf4j
@Entity
@Table(name = "orders") // Quoted because orders is a SQL reserved keyword
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    /**
     * The unique identifier for the order.
     * Generated automatically by the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT")
    private Long id;

    /**
     * Identifier for the customer who placed the order.
     * This could potentially be a foreign key to a Customer entity in a more complex model.
     * Assuming this might link to a Customer entity later
     */
    @Column(name = "customer_id", columnDefinition = "BIGINT")
    private Long customerId;

    /**
     * A brief description or note about the order.
     */
    @Column(name = "description")
    private String description;

    /**
     * The current status of the order (e.g., PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELED).
     * This field is mandatory.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private OrderStatus status;

    /**
     * The total monetary value of the order.
     * This field is mandatory and has a precision of 19 and scale of 4.
     * This amount should ideally be calculated based on the order items.
     */
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    /**
     * Timestamp indicating when the order was created.
     * Automatically set by Hibernate upon creation and cannot be updated.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp indicating when the order was last updated.
     * Automatically set by Hibernate on creation and upon each update.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * A list of items included in this order.
     * Manages a one-to-many relationship with {@link OrderItem}.
     * Changes to this list (add/remove) are cascaded to the database,
     * and orphaned {@link OrderItem}s are automatically removed.
     * Fetched lazily by default.
     */
    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    /**
     * Adds an {@link OrderItem} to this order and sets the bidirectional relationship.
     * Also updates the order's total amount.
     *
     * @param item The {@link OrderItem} to be added.
     */
    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);
    }

    /**
     * Removes an {@link OrderItem} from this order and clears the bidirectional relationship.
     * Also updates the order's total amount.
     *
     * @param item The {@link OrderItem} to be removed.
     */
    public void removeOrderItem(OrderItem item) {
        orderItems.remove(item);
        item.setOrder(null);
    }

    /**
     * Recalculates the {@code totalAmount} of the order based on its current {@link OrderItem}s.
     * Each item's contribution is its unit price multiplied by its quantity.
     * This method should be called whenever order items are added, removed, or their quantities/prices change,
     * or before persisting/updating the order if the total is not managed by database triggers.
     */
    public void calculateTotalAmount() {
        this.totalAmount = orderItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}