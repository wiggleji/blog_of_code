package org.wiggleji.ch02;

import java.util.List;

public class Calculator {
    // Calculator 는 전달받은 숫자의 연산을 담당

    /**
     * get sum of numbers
     * @param numbers int[] numbers to get sum
     * @return sum of numbers
     */
    public int sum(int[] numbers) {
        int sum = 0;
        for (int number : numbers) {
            sum += number;
        }
        return sum;
    }
}
