package org.wiggleji.ch02;

public class StringCalculator {
    // 요구사항: "//;\n1;2;3" 와 같은 문자를 입력받으면
    //  `//` `\n` 사이의 값을 구분자로 인식하고 숫자를 파싱하여 합을 구하는 계산기 구현
    // - 커스텀 구분자 지정 가능
    // - 빈 문자열 혹은 null 값 입력 시 0 반환
    // - 음수 전달 시 RuntimeException 발생
    // -- 메서드는 하나의 책임만 가지도록 (SRP)
    // -- indentation 은 1 depth 만 유지
    // -- else 사용 지양

    // StringCalculator 는 문자값을 받아, 파싱한다
    // 파싱한 결과는 Calculator 에게 계산하도록 위임
    // Calculator 로부터 전달받은 결과값을 반환하도록 처리
}
