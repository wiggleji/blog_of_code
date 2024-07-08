package org.wiggleji.ch02;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringCalculatorTest {

    Calculator calculator;
    StringCalculator stringCalculator;

    @BeforeEach
    public void setUpEach() throws Exception {
        calculator = new Calculator();
        stringCalculator = new StringCalculator();
    }

    @Test
    @DisplayName("parseNumbers() returns Array of int")
    public void testParseNumbers__returns_int_Array() {
        // given
        String[] stringNumbers = {"1", "2", "3", "4", "5", "6", "7", "8", "9"};

        // when
        int[] parsedNumbers = stringCalculator.parseNumbers(stringNumbers);

        // then
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, parsedNumbers);
        // check sum for validation
        assertEquals(calculator.sum(parsedNumbers), 45);
    }

    @Test
    @DisplayName("parseNumbers() throws RuntimeException on negative number")
    public void testParseNumbers__throws_RuntimeException() {
        // given
        String[] stringNumbers = {"1", "2", "3", "4", "5", "6", "7", "8", "-9"};

        // when

        // then
        assertThrows(RuntimeException.class, () -> stringCalculator.parseNumbers(stringNumbers));
    }

    @Test
    @DisplayName("getParsedPositiveInt() returns int")
    public void testGetParsedPositiveInt__returns_int() {
        // given
        String stringNumber = "1";

        // when
        int parsedPositiveInt = stringCalculator.getParsedPositiveInt(stringNumber);

        // then
        assertEquals(parsedPositiveInt, 1);
    }

    @Test
    @DisplayName("getParsedPositiveInt() throws RuntimeException on negative number")
    public void testGetParsedPositiveInt__throws_RuntimeException() {
        // given
        String stringNumber = "-1";

        // when

        // then
        assertThrows(RuntimeException.class, () -> stringCalculator.getParsedPositiveInt(stringNumber));
    }

    @Test
    @DisplayName("parseStringNumbers() returns Array of String")
    public void testParseStringNumbers__commas_returns_String_Array() {
        // given
        String stringNumbers = "//,//1,2,3,4,5,6,7,8,9";
        String[] parsedStringNumbers = {"1", "2", "3", "4", "5", "6", "7", "8", "9"};

        // when
        String[] parsedNumbers = stringCalculator.parseStringNumbers(stringNumbers);

        // then
        assertEquals(parsedNumbers.length, 9);
        for (int i = 0; i < parsedNumbers.length; i++) {
            assertEquals(parsedNumbers[i], parsedStringNumbers[i]);
        }
    }

    @Test
    @DisplayName("parseStringNumbers() returns Array of String")
    public void testParseStringNumbers__semicolon_returns_String_Array() {
        // given
        String stringNumbers = "//;//1;2;3;4;5;6;7;8;9";
        String[] parsedStringNumbers = {"1", "2", "3", "4", "5", "6", "7", "8", "9"};

        // when
        String[] parsedNumbers = stringCalculator.parseStringNumbers(stringNumbers);

        // then
        assertEquals(parsedNumbers.length, 9);
        for (int i = 0; i < parsedNumbers.length; i++) {
            assertEquals(parsedNumbers[i], parsedStringNumbers[i]);
        }
    }

    @Test
    @DisplayName("parseStringNumbers() returns null on invalid input")
    public void testParseStringNumbers__returns_null_invalid_input() {
        // given
        String stringNumbers = "//,\\1,2,3,4,5,6,7,8,9";

        // when
        String[] parsedNumbers = stringCalculator.parseStringNumbers(stringNumbers);

        // then
        assertNull(parsedNumbers);
    }
}