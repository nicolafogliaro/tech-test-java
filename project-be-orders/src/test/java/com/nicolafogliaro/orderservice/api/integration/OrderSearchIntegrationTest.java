package com.nicolafogliaro.orderservice.api.integration;

import com.nicolafogliaro.orderservice.api.controller.PagedOrderResponse;
import com.nicolafogliaro.orderservice.api.dto.order.OrderResponse;
import com.nicolafogliaro.orderservice.api.dto.order.OrderSearchCriteria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/orders.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class OrderSearchIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private final String BASE_URL = "/api/v1/orders/search";

    private <T> HttpEntity<T> buildJsonRequest(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @Test
    public void searchOrders_withTextQuery_shouldReturnMatchingOrders() {

        OrderSearchCriteria criteria = OrderSearchCriteria.builder()
                .query("laptop")
                .page(0)
                .size(10)
                .sort("createdAt")
                .direction("desc")
                .build();

        ResponseEntity<PagedOrderResponse> response = restTemplate.exchange(
                BASE_URL,
                HttpMethod.POST,
                buildJsonRequest(criteria),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<OrderResponse> content = response.getBody().getContent();

        assertThat(content).isNotEmpty();
        assertThat(content.get(0).description().toLowerCase()).contains("laptop");
    }

    @Test
    public void searchOrders_withCustomerId_shouldReturnCustomerOrders() {

        OrderSearchCriteria criteria = OrderSearchCriteria.builder()
                .customerId(100L)
                .page(0)
                .size(10)
                .build();

        ResponseEntity<PagedOrderResponse> response = restTemplate.exchange(
                BASE_URL,
                HttpMethod.POST,
                buildJsonRequest(criteria),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).allMatch(o -> o.customerId().equals(100L));
    }

    @Test
    public void searchOrders_withDateRange_shouldReturnMatchingOrders() {
        OrderSearchCriteria criteria = OrderSearchCriteria.builder()
                .startDate(LocalDate.of(2023, 10, 1))
                .endDate(LocalDate.of(2023, 10, 2))
                .build();

        ResponseEntity<PagedOrderResponse> response = restTemplate.exchange(
                BASE_URL,
                HttpMethod.POST,
                buildJsonRequest(criteria),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).allMatch(order ->
                !order.createdAt().toLocalDate().isBefore(LocalDate.of(2023, 10, 1)) &&
                        !order.createdAt().toLocalDate().isAfter(LocalDate.of(2023, 10, 2))
        );
    }

    @Test
    public void searchOrders_withNoMatch_shouldReturnEmptyList() {
        OrderSearchCriteria criteria = OrderSearchCriteria.builder()
                .query("nonexistentword")
                .page(0)
                .size(10)
                .build();

        ResponseEntity<PagedOrderResponse> response = restTemplate.exchange(
                BASE_URL,
                HttpMethod.POST,
                buildJsonRequest(criteria),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).isEmpty();
    }
}

