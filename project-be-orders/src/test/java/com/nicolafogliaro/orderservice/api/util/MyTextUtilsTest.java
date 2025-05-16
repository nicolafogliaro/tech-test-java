package com.nicolafogliaro.orderservice.api.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class MyTextUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
    @NullAndEmptySource
    void isEmptyPositive(String value) {
        Assertions.assertTrue(MyTextUtils.isEmpty(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "1", "a\t", "1\n"})
    void isEmptyNegative(String value) {
        Assertions.assertFalse(MyTextUtils.isEmpty(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "1", "a\t", "1\n"})
    void nonEmptyPositive(String value) {
        Assertions.assertTrue(MyTextUtils.nonEmpty(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
    @NullAndEmptySource
    void nonEmptyNegative(String value) {
        Assertions.assertFalse(MyTextUtils.nonEmpty(value));
    }

}
