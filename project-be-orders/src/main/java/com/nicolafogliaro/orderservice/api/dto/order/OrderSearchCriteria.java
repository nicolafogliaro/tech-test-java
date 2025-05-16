package com.nicolafogliaro.orderservice.api.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.nicolafogliaro.orderservice.api.model.order.OrderColumnNameForSearch.CREATED_AT;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Criteria for searching orders.")
public class OrderSearchCriteria {

    /**
     * The unique identifier of the customer whose orders are to be searched.
     * If null, orders for all customers (matching other criteria) may be considered.
     */
    @Schema(description = "The unique identifier of the customer.", example = "12345")
    @Positive(message = "Customer ID must be a positive number if provided.")
    private Long customerId;


    /**
     * A general query string for text-based search.
     * This can be used to search against fields like product name, order description, etc.
     */
    @Schema(description = "Query string for text search (e.g., product name, order description).", example = "IPhone")
    private String query;

    /**
     * The start date for the search range (inclusive).
     * Orders created on or after this date will be included.
     * The date should be in ISO DATE format (yyyy-MM-dd).
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Start date for the search range (inclusive), in yyyy-MM-dd format.", example = "2025-01-01")
    private LocalDate startDate;

    /**
     * The end date for the search range (inclusive).
     * Orders created on or before this date will be included.
     * The date should be in ISO DATE format (yyyy-MM-dd).
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "End date for the search range (inclusive), in yyyy-MM-dd format.", example = "2025-12-31")
    private LocalDate endDate;

    /**
     * The page number for pagination (0-indexed).
     * Defaults to 0 if not specified.
     */
    @Builder.Default
    @Schema(description = "Page number for pagination (0-indexed).", defaultValue = "0", example = "0")
    @Min(value = 0, message = "Page number must be 0 or greater.")
    private Integer page = 0;

    /**
     * The number of items per page for pagination.
     * Defaults to 20 if not specified.
     */
    @Builder.Default
    @Schema(description = "Number of items per page.", defaultValue = "20", example = "10")
    @Min(value = 1, message = "Page size must be at least 1.")
    @Max(value = 100, message = "Page size must not exceed 100.")
    private Integer size = 20;

    /**
     * The field to sort the results by.
     * It is assumed that a constant {@code CREATED_AT} is defined elsewhere,
     * representing the default sort field (e.g., "createdAt").
     * Defaults to the value of {@code CREATED_AT}.
     */
    @Builder.Default
    @Schema(description = "Field to sort the results by.", defaultValue = "createdAt", example = "createdAt")
    private String sort = CREATED_AT;

    /**
     * The direction of sorting.
     * Can be "asc" for ascending or "desc" for descending.
     * Defaults to "desc".
     */
    @Builder.Default
    @Schema(description = "Sort direction.", defaultValue = "desc", example = "asc", allowableValues = {"asc", "desc"})
    @Pattern(regexp = "asc|desc", message = "Sort direction must be 'asc' or 'desc'.")
    private String direction = "desc";
}