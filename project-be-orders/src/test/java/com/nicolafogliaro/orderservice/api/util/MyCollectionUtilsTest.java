package com.nicolafogliaro.orderservice.api.util;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MyCollectionUtilsTest {

    private List<Object> emptyList;
    private List<Object> filledList;

    @BeforeEach
    void init() {
        emptyList = new ArrayList<>();
        filledList = List.of("", 3);
    }

    @Test
    void isEmpty() {
        Assertions.assertTrue(MyCollectionUtils.isEmpty(emptyList));
        Assertions.assertFalse(MyCollectionUtils.isEmpty(filledList));
    }

    @Test
    void nonEmpty() {
        Assertions.assertFalse(MyCollectionUtils.nonEmpty(emptyList));
        Assertions.assertTrue(MyCollectionUtils.nonEmpty(filledList));
    }
}
