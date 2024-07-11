package org.wiggleji.ch03.webserver;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class WebServer {
    private static final Logger log = Logger.getLogger(WebServer.class.getName());
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws Exception {
        int port = 0;
        // args 로 전달받은 port 존재 시 설정
        if (args == null || args.length == 0) {
            port = DEFAULT_PORT;
        } else {
            port = Integer.parseInt(args[0]);
        }

        // 서버 소켓 생성. 기본 포트: 8080 사용
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Web server started on port: " + port);

            // 클라이언트 연결까지 대기
            Socket connection;
            while ((connection = serverSocket.accept()) != null) {
                RequestHandler requestHandler = new RequestHandler(connection);
                requestHandler.start();
            }
        }
    }
}
