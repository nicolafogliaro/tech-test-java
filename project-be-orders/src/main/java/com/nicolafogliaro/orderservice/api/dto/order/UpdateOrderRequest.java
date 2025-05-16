package com.nicolafogliaro.orderservice.api.dto.order;

import com.nicolafogliaro.orderservice.api.dto.orderitem.OrderItemRequest;
import com.nicolafogliaro.orderservice.api.model.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Data Transfer Object for updating an existing order.
 * <p>
 * This record allows for partial updates to an order. Any field provided will be
 * considered for an update, while fields left as {@code null} (or not included in the JSON payload)
 * are generally intended to be ignored, leaving the corresponding property on the
 * order unchanged. The specific behavior for handling {@code null} vs. absent fields
 * depends on the service implementation.
 * </p>
 * <p>
 * The {@code items} list, if provided, is typically treated as a full replacement
 * of the existing items for the order. An empty list might signify removal of all items,
 * while a {@code null} list would mean no change to the items.
 * </p>
 *
 * @param customerId       The optional new customer ID to associate with the order.
 *                         If {@code null}, the customer ID is typically not updated.
 * @param orderDescription The optional new description for the order.
 *                         If {@code null}, the description is typically not updated.
 * @param status           The optional new {@link OrderStatus} for the order.
 *                         If {@code null}, the status is typically not updated.
 * @param items            An optional list of {@link OrderItemRequest} objects.
 *                         If provided, this list is intended to replace all existing items
 *                         in the order. Each item in the list will be validated if the list
 *                         is not {@code null}. If {@code null}, items are typically not updated.
 *                         An empty list might mean "remove all items".
 */
@Schema(name = "UpdateOrderRequest",
        description = "Payload for updating an existing order. All fields are optional. " +
                "Only provided fields will be considered for updating the order resource. " +
                "Providing a new 'items' list replaces all existing items.")
public record UpdateOrderRequest(

        @Schema(description = "The new unique identifier of the customer associated with this order. " +
                "If not provided or null, the customer association remains unchanged.",
                example = "456",
                nullable = true) // Indicates the field can be explicitly set to null or omitted
        Long customerId,

        @Schema(description = "A new descriptive text for the order. " +
                "If not provided or null, the order description remains unchanged.",
                example = "Updated order: please ensure gift wrapping for item X.",
                nullable = true)
        String orderDescription,

        @Schema(description = "The new status of the order. " +
                "If not provided or null, the order status remains unchanged.",
                implementation = OrderStatus.class, // Helps Swagger UI list enum values if OrderStatus is annotated
                example = "PROCESSING",
                nullable = true)
        OrderStatus status, // Assuming OrderStatus is an enum

        @ArraySchema(
                schema = @Schema(
                        description = "A list of order items. If provided, this list will replace all existing items " +
                                "associated with the order. An empty list will remove all items. " +
                                "If null or not provided, the items remain unchanged.",
                        implementation = OrderItemRequest.class // Crucial for linking to the OrderItemRequest schema
                ),
                minItems = 0, // An empty list is permissible (e.g., to remove all items)
                uniqueItems = false, // Usually, order items don't need to be unique by themselves
                arraySchema = @Schema(nullable = true) // The list itself can be null or not provided
        )
        @Valid // Jakarta Bean Validation: if 'items' list is present, its elements will be validated.
        List<OrderItemRequest> items
) {}

