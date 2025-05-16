package com.nicolafogliaro.orderservice.api.service.impl;

import com.nicolafogliaro.orderservice.api.dto.order.OrderResponse;
import com.nicolafogliaro.orderservice.api.dto.order.OrderSearchCriteria;
import com.nicolafogliaro.orderservice.api.model.order.Order;
import com.nicolafogliaro.orderservice.api.model.order.OrderStatus;
import com.nicolafogliaro.orderservice.api.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// -----------------------------------------------------------------------------
// Service-Level Unit Tests
// -----------------------------------------------------------------------------
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderSearchServiceImpl Tests")
class OrderSearchServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderSearchServiceImpl orderSearchService;

    private OrderSearchCriteria defaultCriteria;

    @BeforeEach
    void setUp() {
        defaultCriteria = new OrderSearchCriteria(); // Uses default values
    }

//    @Test
//    @DisplayName("searchOrdersWithDb should use default criteria for pageable and sort when none provided")
//    void searchOrdersWithDb_usesDefaultCriteria() {
//        Pageable expectedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate"));
//        Order order1 = OrderTestFactory.createOrder(1L, 101L, "Default Order 1", OrderStatus.PENDING, BigDecimal.valueOf(100), LocalDateTime.now().minusDays(2));
//        Page<Order> mockedPage = new PageImpl<>(List.of(order1), expectedPageable, 1);
//
//        when(orderRepository.findAll(any(Specification.class), eq(expectedPageable))).thenReturn(mockedPage);
//
//        Page<OrderResponse> result = orderSearchService.searchOrdersWithDb(defaultCriteria);
//
//        assertThat(result).isNotNull();
//        assertThat(result.getContent()).hasSize(1);
//        assertThat(result.getContent().get(0).id()).isEqualTo(1L);
//        assertThat(result.getPageable().getPageNumber()).isEqualTo(0);
//        assertThat(result.getPageable().getPageSize()).isEqualTo(20);
//        assertThat(result.getSort().getOrderFor("orderDate").getDirection()).isEqualTo(Sort.Direction.DESC);
//
//        verify(orderRepository).findAll(any(Specification.class), eq(expectedPageable));
//    }
//
//    @Test
//    @DisplayName("searchOrdersWithDb should correctly map Page<Order> to Page<OrderResponse>")
//    void searchOrdersWithDb_mapsPageCorrectly() {
//        OrderSearchCriteria criteria = new OrderSearchCriteria();
//        criteria.setPage(0);
//        criteria.setSize(1);
//        criteria.setSort("id");
//        criteria.setDirection("asc");
//        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "id"));
//
//        Order order = OrderTestFactory.createOrder(1L, 101L, "Mapping Test", OrderStatus.CONFIRMED, BigDecimal.valueOf(123.45), LocalDateTime.now());
//        Page<Order> repoPage = new PageImpl<>(List.of(order), pageable, 1);
//
//        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(repoPage);
//
//        Page<OrderResponse> result = orderSearchService.searchOrdersWithDb(criteria);
//
//        assertThat(result.getTotalElements()).isEqualTo(1);
//        assertThat(result.getContent()).hasSize(1);
//        OrderResponse response = result.getContent().get(0);
//        assertThat(response.id()).isEqualTo(order.getId());
//        assertThat(response.customerId()).isEqualTo(order.getCustomerId());
//        assertThat(response.description()).isEqualTo(order.getDescription());
//        assertThat(response.status()).isEqualTo(order.getStatus());
//        assertThat(response.totalAmount()).isEqualTo(order.getTotalAmount());
//        assertThat(response.createdAt()).isEqualTo(order.getCreatedAt());
//    }
//
//    @Test
//    @DisplayName("searchOrdersWithDb should handle empty result from repository")
//    void searchOrdersWithDb_handlesEmptyResult() {
//        Pageable expectedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate"));
//        Page<Order> emptyPage = Page.empty(expectedPageable);
//
//        when(orderRepository.findAll(any(Specification.class), eq(expectedPageable))).thenReturn(emptyPage);
//
//        Page<OrderResponse> result = orderSearchService.searchOrdersWithDb(defaultCriteria);
//
//        assertThat(result).isNotNull();
//        assertThat(result.getContent()).isEmpty();
//        assertThat(result.getTotalElements()).isZero();
//    }
//
//    @Test
//    @DisplayName("searchOrdersWithDb should pass constructed Specification to repository")
//    void searchOrdersWithDb_passesSpecificationToRepository() {
//        OrderSearchCriteria criteria = new OrderSearchCriteria();
//        criteria.setCustomerId(123L);
//        criteria.setQuery("specific query");
//        criteria.setStartDate(LocalDate.of(2024, 1, 1));
//
//        Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getSize(), Sort.by(Sort.Direction.fromString(criteria.getDirection()), criteria.getSort()));
//        Page<Order> emptyPage = Page.empty(pageable);
//        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);
//
//        orderSearchService.searchOrdersWithDb(criteria);
//
//        ArgumentCaptor<Specification<Order>> specCaptor = ArgumentCaptor.forClass(Specification.class);
//        verify(orderRepository).findAll(specCaptor.capture(), eq(pageable));
//
//        // We can't easily inspect the captured spec without executing it or making OrderSpecifications mockable.
//        // However, we know *a* specification was passed.
//        assertThat(specCaptor.getValue()).isNotNull();
//        // For a more robust test of OrderSpecifications.withSearchCriteria itself,
//        // that class would need its own dedicated unit tests with mocked CriteriaBuilder, Root, etc.
//    }
//
//    @Test
//    @DisplayName("searchOrdersWithDb should use custom sort and direction")
//    void searchOrdersWithDb_usesCustomSortAndDirection() {
//        OrderSearchCriteria criteria = new OrderSearchCriteria();
//        criteria.setSort("totalAmount");
//        criteria.setDirection("asc");
//        criteria.setPage(1);
//        criteria.setSize(5);
//
//        Pageable expectedPageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.ASC, "totalAmount"));
//        Page<Order> mockedPage = new PageImpl<>(Collections.emptyList(), expectedPageable, 0);
//
//        when(orderRepository.findAll(any(Specification.class), eq(expectedPageable))).thenReturn(mockedPage);
//
//        orderSearchService.searchOrdersWithDb(criteria);
//
//        verify(orderRepository).findAll(any(Specification.class), eq(expectedPageable));
//    }
}