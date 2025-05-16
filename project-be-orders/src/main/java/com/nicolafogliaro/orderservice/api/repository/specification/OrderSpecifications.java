package com.nicolafogliaro.orderservice.api.repository.specification;

import com.nicolafogliaro.orderservice.api.dto.order.OrderSearchCriteria;
import com.nicolafogliaro.orderservice.api.model.OrderItem;
import com.nicolafogliaro.orderservice.api.model.Product;
import com.nicolafogliaro.orderservice.api.model.order.Order;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static com.nicolafogliaro.orderservice.api.model.order.OrderColumnNameForSearch.CREATED_AT;

/**
 * Specifications class for building dynamic JPA queries for Order entities.
 * Provides flexible and type-safe criteria for filtering orders based on
 * various search parameters.
 */
@Slf4j
public final class OrderSpecifications {

    /**
     * Creates a JPA Specification for Order entities based on the provided search criteria.
     * This specification can be used to filter orders by text content (name/description)
     * and date range.
     *
     * @param criteria The search criteria containing filters to apply
     * @return A Specification that can be used with Spring Data JPA repositories
     */
    public static Specification<Order> withSearchCriteria(OrderSearchCriteria criteria) {

        log.debug(">>> [OrderSpecifications#withSearchCriteria] criteria: {}", criteria);

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            addCustomerIdPredicate(cb, root, predicates, criteria);
            addDateRangePredicates(cb, root, predicates, criteria);
            addTextSearchPredicates(cb, root, query, predicates, criteria);

            Predicate and = cb.and(predicates.toArray(new Predicate[0]));
            log.debug("<<< [OrderSpecifications#withSearchCriteria] res: {}", and);
            return and;
        };
    }

    private static void addCustomerIdPredicate(
            CriteriaBuilder cb,
            Root<Order> root,
            List<Predicate> predicates,
            OrderSearchCriteria criteria) {

        if (criteria.getCustomerId() != null) {
            predicates.add(cb.equal(root.get("customerId"), criteria.getCustomerId()));
        }
    }

    private static void addDateRangePredicates(
            CriteriaBuilder cb,
            Root<Order> root,
            List<Predicate> predicates,
            OrderSearchCriteria criteria) {

        if (criteria.getStartDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                    root.get(CREATED_AT), criteria.getStartDate().atStartOfDay()));
        }

        if (criteria.getEndDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(
                    root.get(CREATED_AT), criteria.getEndDate().atTime(LocalTime.MAX)));
        }
    }

    private static void addTextSearchPredicates(
            CriteriaBuilder cb,
            Root<Order> root,
            CriteriaQuery<?> query,
            List<Predicate> predicates,
            OrderSearchCriteria criteria) {

        if (StringUtils.hasText(criteria.getQuery())) {

            String searchTerm = "%" + criteria.getQuery().toLowerCase() + "%";

            Predicate orderDesc = cb.like(cb.lower(root.get("description")), searchTerm);

            // Join with OrderItem, then with Product to filter by product description
            Join<Order, OrderItem> orderItemJoin = root.join("orderItems", JoinType.INNER);
            Join<OrderItem, Product> productJoin = orderItemJoin.join("product", JoinType.INNER);

            Predicate productDesc = cb.like(cb.lower(productJoin.get("description")), searchTerm);
            Predicate productName = cb.like(cb.lower(productJoin.get("name")), searchTerm);

            predicates.add(cb.or(orderDesc, productDesc, productName));

            query.distinct(true); // Prevent duplicate orders if multiple items match
        }

    }

    private OrderSpecifications() {}

}
