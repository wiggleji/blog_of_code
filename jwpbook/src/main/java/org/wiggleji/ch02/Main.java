package org.wiggleji.ch02;

import java.util.Arrays;

public class Main {
    // 요구사항: "//;\n1;2;3" 와 같은 문자를 입력받으면
    //  `//` `\n` 사이의 값을 구분자로 인식하고 숫자를 파싱하여 합을 구하는 계산기 구현
    // - 커스텀 구분자 지정 가능
    // - 빈 문자열 혹은 null 값 입력 시 0 반환
    // - 음수 전달 시 RuntimeException 발생
    // -- 메서드는 하나의 책임만 가지도록 (SRP)
    // -- indentation 은 1 depth 만 유지
    // -- else 사용 지양

    private final Calculator calculator;
    private final StringCalculator stringCalculator;

    public Main(StringCalculator stringCalculator, Calculator calculator) {
        this.calculator = calculator;
        this.stringCalculator = stringCalculator;
    }

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            System.out.printf("Your targetString: %s | sum: %d%n", Arrays.toString(args), 0);
            return;
        }

        String targetString = args[0];

        Calculator calculator = new Calculator();
        StringCalculator stringCalculator = new StringCalculator();

        Main main = new Main(stringCalculator, calculator);

        String[] stringNumbers = main.stringCalculator.parseStringNumbers(targetString);
        int[] numbers = main.stringCalculator.parseNumbers(stringNumbers);
        int sum = calculator.sum(numbers);
        System.out.printf("Your targetString: %s | sum: %d%n", targetString, sum);
    }
}
