package com.wiggleji.sslmle.config

import com.wiggleji.sslmle.config.logging.LogIdProvider
import feign.Logger
import feign.Request
import feign.Response
import feign.Retryer
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.cloud.openfeign.FeignFormatterRegistrar
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.util.StreamUtils
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Feign 공통 Configuration
 * - Feign request/response logging
 * - Feign request retryer
 */
@Configuration
@EnableFeignClients(basePackages = ["com.wiggleji.sslmle.infra.feign"])
class FeignConfig {

    @Bean
    fun feignLoggerLevel(): Logger.Level = Logger.Level.BASIC

    @Bean
    fun feignLogger(logIdProvider: LogIdProvider): Logger = FeignClientLogger(logIdProvider)

    @Bean
    fun feignDateTimeFormatterRegister(): FeignFormatterRegistrar {
        return FeignFormatterRegistrar { registry ->
            val registrar = org.springframework.format.datetime.standard.DateTimeFormatterRegistrar()
            registrar.setUseIsoFormat(true)
            registrar.registerFormatters(registry)
        }
    }

    /**
     * Feign Client Logger
     * Feign 요청 및 응답에 대한 상세 로그를 기록
     * - HTTP URI, Method, Header, Body 포함
     * - LogIdProvider를 사용하여 로그 식별자 주입
     * @see LogIdProvider
     */
    class FeignClientLogger(
        private val logIdProvider: LogIdProvider
    ) : feign.Logger() {
        companion object {
            private val log = org.slf4j.LoggerFactory.getLogger(FeignClientLogger::class.java)
        }

        override fun logRequest(configKey: String, logLevel: Logger.Level, request: Request) {
            val logId = logIdProvider.currentOrNew()
            val stringBody = createRequestStringBody(request)
            log.info("[{}][Feign Request] URI: {}, Method: {}, Header:{}, Body: {}", logId, request.url(), request.httpMethod(), request.headers(), stringBody)
            if (logLevel.ordinal >= Logger.Level.HEADERS.ordinal) {
                super.logRequest(configKey, logLevel, request)
            }
        }

        private fun createRequestStringBody(request: Request): String {
            val body = request.body() ?: return ""
            val charset = request.charset() ?: StandardCharsets.UTF_8
            return String(body, charset)
        }

        @Throws(IOException::class)
        protected override fun logAndRebufferResponse(
            configKey: String?,
            logLevel: Logger.Level,
            response: Response,
            elapsedTime: Long
        ): Response {
            val byteArray = getResponseBodyByteArray(response)
            log.info(
                "[{}][Feign Response] URI: {}, Status: {}, Body:{} ",
                logIdProvider.currentOrNew(),
                response.request().url(),
                HttpStatus.valueOf(response.status()),
                String(byteArray, StandardCharsets.UTF_8)
            )
            val rebuiltResponse = response.toBuilder().body(byteArray).build()
            val result = if (logLevel.ordinal >= Logger.Level.HEADERS.ordinal) {
                super.logAndRebufferResponse(configKey, logLevel, rebuiltResponse, elapsedTime)
            } else {
                rebuiltResponse
            }
            return result
        }

        @Throws(IOException::class)
        private fun getResponseBodyByteArray(response: Response): ByteArray {
            if (response.body() == null) {
                return byteArrayOf()
            }

            return StreamUtils.copyToByteArray(response.body().asInputStream())
        }

        override fun log(configKey: String, format: String, vararg args: Any) {
            log.debug(format(configKey, format, *args));
        }

        protected fun format(configKey: String, format: String?, vararg args: Any?): String {
            return String.format(methodTag(configKey) + format, *args)
        }
    }

    /**
     * Feign Retryer 설정
     * - 초기 대기 시간: 100ms
     * - 최대 대기 시간: 3초
     * - 최대 재시도 횟수: 5회
     */
    @Bean
    fun retryer(): Retryer {
        return Retryer.Default(100L, TimeUnit.SECONDS.toMillis(3), 5)
    }
}
