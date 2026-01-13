package com.wiggleji.sslmle.config.logging

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class LogIdInterceptor(
    private val logIdProvider: LogIdProvider
) : HandlerInterceptor {

    /**
     * HTTP 요청이 들어올 때마다 실행되며
     * 요청 헤더(LogIdProvider.LOG_ID_HEADER) 식별자 혹은 새로운 로그 식별자를
     * LogIdProvider에 설정
     * @see LogIdProvider
     * @return true (요청 처리를 계속 진행)
     */
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val existing = logIdProvider.current()
        if (existing == null) {
            val incoming = request.getHeader(LogIdProvider.LOG_ID_HEADER)
            logIdProvider.fromHeaderOrNew(incoming)
        }
        return true
    }

    /**
     * HTTP 요청 처리가 완료된 후 실행되며
     * LogIdProvider에 설정된 로그 식별자 제거
     * @see LogIdProvider
     */
    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        logIdProvider.clear()
    }
}
