package com.wiggleji.sslmle.config.logging

import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Slf4j MDC 기반의 로그 식별자 제공자
 * 스레드 로컬에 UUID 형식의 로그 식별자를 저장 및 제공
 * HTTP, non-HTTP(스케줄러 등) 환경 모두에서 사용 가능
 * LogIdProvider 를 의존성 주입 후,
 * LogIdProvider.current() 혹은 LogIdProvider.currentOrNew() 로 현재 스레드의 로그 식별자 조회
 */
@Component
class LogIdProvider {
    companion object {
        const val LOG_ID_KEY = "logId"
        const val LOG_ID_HEADER = "X-Request-Id"
    }

    /**
     * 현재 스레드의 로그 식별자 조회
     * @return 현재 스레드의 로그 식별자, 없으면 null
     */
    fun current(): String? = MDC.get(LOG_ID_KEY)

    /**
     * 현재 스레드의 로그 식별자 조회, 없으면 새로 생성하여 설정
     * @return 현재 스레드의 로그 식별자
     */
    fun currentOrNew(): String = current() ?: generateAndSet()

    /**
     * 현재 스레드의 로그 식별자 설정
     * @param logId 설정할 로그 식별자
     */
    fun set(logId: String) {
        MDC.put(LOG_ID_KEY, logId)
    }

    /**
     * 현재 스레드의 로그 식별자 제거
     */
    fun clear() {
        MDC.remove(LOG_ID_KEY)
    }

    /**
     * 헤더에서 로그 식별자 조회, 없으면 새로 생성하여 설정
     * @param headerValue 헤더에서 조회한 로그 식별자 값
     * @return 현재 스레드의 로그 식별자
     */
    fun fromHeaderOrNew(headerValue: String?): String {
        val logId = headerValue?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        set(logId)
        return logId
    }

    /**
     * 새 로그 식별자 생성 및 설정
     * @return 생성된 로그 식별자 (UUID)
     */
    private fun generateAndSet(): String {
        val logId = UUID.randomUUID().toString()
        set(logId)
        return logId
    }
}
