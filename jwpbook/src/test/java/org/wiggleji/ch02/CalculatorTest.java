package org.wiggleji.ch02;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {

    @Test
    void testAdd() {
        // given
        Calculator calc = new Calculator();
        int[] testNumbers = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

        // when
        int sum = calc.sum(testNumbers);

        // then
        Assertions.assertEquals(sum, 55);
    }
}