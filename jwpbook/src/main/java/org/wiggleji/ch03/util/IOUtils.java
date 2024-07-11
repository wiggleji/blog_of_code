package org.wiggleji.ch03.util;

import java.io.BufferedReader;
import java.io.IOException;

public class IOUtils {

    /**
     * BufferReader 로부터 Request Body 를 읽어온다.
     * @param br    [BufferedReader] Request body 를 읽어오기 위한 버퍼
     * @param contentLength     [int] Request header 의 Content-Length
     * @return  Request body 값
     * @throws IOException
     */
    public static String readData(BufferedReader br, int contentLength) throws IOException {
        char[] body = new char[contentLength];
        br.read(body, 0, contentLength);
        return String.copyValueOf(body);
    }

}
