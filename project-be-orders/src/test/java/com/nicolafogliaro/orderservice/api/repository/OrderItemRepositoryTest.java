package com.nicolafogliaro.orderservice.api.repository;

import com.nicolafogliaro.orderservice.api.model.OrderItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.jdbc.SqlGroup;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focuses on JPA components, provides an in-memory DB by default if not configured otherwise
 */
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderItemRepositoryTest {

    @Autowired
    private OrderItemRepository orderItemRepository; // Your JPA repository


    @Test
    @Sql(scripts = "/sql/orders.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void shouldFindOrderItemsAfterScriptPopulation() {

        List<OrderItem> items = orderItemRepository.findAll();
        assertThat(items).hasSize(2);

        OrderItem item1 = items.stream().filter(oi -> oi.getId().equals(1L)).findFirst().orElse(null);
        assertThat(item1).isNotNull();
        assertThat(item1.getProduct().getId()).isEqualTo(10L);
        assertThat(item1.getQuantity()).isEqualTo(1);

        OrderItem item2 = items.stream().filter(oi -> oi.getId().equals(2L)).findFirst().orElse(null);
        assertThat(item2).isNotNull();
        assertThat(item2.getProduct().getId()).isEqualTo(11L);
        assertThat(item2.getQuantity()).isEqualTo(2);
    }

    @Test
        // No @Sql here, so the database should be empty or only have data from class-level @Sql
        // (or from spring.datasource.initialization-mode if used and ddl-auto wasn't create-drop)
    void shouldBeEmptyIfNoDataScriptIsRun() {
        List<OrderItem> items = orderItemRepository.findAll();
        assertThat(items).isEmpty();
    }

    @Test
    @SqlGroup({
            @Sql(value = "/sql/orders.sql",
                    config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
                    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
            @Sql(value = "/sql/cleanup.sql",
                    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    })
    void anotherTestWithSpecificData() {
        List<OrderItem> items = orderItemRepository.findAllById(List.of(1L)); // Assuming such a method exists
        assertThat(items).hasSize(1);
    }

}