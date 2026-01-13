package com.wiggleji.sslmle.config.mle

import feign.RequestInterceptor
import feign.RequestTemplate
import mu.KotlinLogging
import java.nio.charset.StandardCharsets

private val log = KotlinLogging.logger {}

/**
 * Feign 요청 interceptor. MLE 암호화를 요청 본문에 적용
 */
class MleRequestInterceptor(
    private val encryptor: MleEncryptor,
    private val properties: MleProperties
) : RequestInterceptor {

    /**
     * Feign HTTP RequestTemplate 오버라이드
     * - 요청 본문에 MLE 암호화 적용
     * - GET 및 DELETE 요청은 건너뜀
     * - 본문이 비어있는 요청은 건너뜀
     * - 이미 암호화된 본문은 건너뜀
     */
    override fun apply(template: RequestTemplate) {
        val method = template.method().uppercase()
        if (method == "GET" || method == "DELETE") {
            return
        }

        val requestBody = template.requestBody() ?: return
        val bodyBytes = requestBody.asBytes()
        if (bodyBytes == null || bodyBytes.isEmpty()) {
            log.debug { "[MleRequestInterceptor] Skipping - empty body for ${template.method()} ${template.url()}" }
            return
        }

        val payloadString = String(bodyBytes, StandardCharsets.UTF_8)
        if (payloadString.contains(MleEnvelope.ENC_DATA_FIELD)) {
            log.debug { "[MleRequestInterceptor] Skipping - already encrypted for ${template.method()} ${template.url()}" }
            return
        }

        val currentContentType = template.headers()["Content-Type"]?.firstOrNull()
        val envelopeJson = encryptor.encryptToJson(payloadString, currentContentType)

        template.body(envelopeJson.toByteArray(), StandardCharsets.UTF_8)
        template.removeHeader("Content-Length")
        template.header("Content-Type", "application/json")

        val keyId = properties.keyId ?: error("[MleRequestInterceptor] mle.key-id must be provided")
        template.header(properties.keyIdHeaderName, keyId)

        log.debug { "[MleRequestInterceptor] Encrypted request for ${template.method()} ${template.url()}" }
    }
}
