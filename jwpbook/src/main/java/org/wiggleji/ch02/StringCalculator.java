package org.wiggleji.ch02;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringCalculator {
    // StringCalculator 는 문자값을 받아, 파싱한다
    // 파싱한 결과는 Calculator 에게 계산하도록 위임
    // Calculator 로부터 전달받은 결과값을 반환하도록 처리

    public int[] parseNumbers(String[] stringNumbers) {
        int[] result = new int[stringNumbers.length];
        for (int i = 0; i < stringNumbers.length; i++) {
            result[i] = getParsedPositiveInt(stringNumbers[i]);
        }
        return result;
    }

    public int getParsedPositiveInt(String stringNumbers) {
        int parsedInt = Integer.parseInt(stringNumbers);
        if (parsedInt < 0) throw new RuntimeException("numbers should be larger than 0");
        return parsedInt;
    }

    public String[] parseStringNumbers(String numbers) {
        Matcher matcher = Pattern.compile("//(.)//(.*)").matcher(numbers);
        if (matcher.find()) {
            String customDelimiter = matcher.group(1);
            return matcher.group(2).split(customDelimiter);
        }
        return null;
    }
}