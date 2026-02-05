package com.wiggleji.sslmle.config.mle

import feign.Response
import feign.codec.DecodeException
import feign.codec.Decoder
import mu.KotlinLogging
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets

private val log = KotlinLogging.logger {}

/**
 * MLE 응답 디코더
 * 응답 본문에 MLE 암호화가 적용된 경우 복호화 수행
 * MleEnvelope.ENC_DATA_FIELD(`encData`) 필드가 포함된 경우 복호화 시도
 */
class MleResponseDecoder(
    private val delegate: Decoder,
    private val decryptor: MleDecryptor
) : Decoder {

    /**
     * Feign 응답 디코더 오버라이드
     * - 응답 본문에 MLE 암호화가 적용된 경우 복호화 수행
     * - 본문이 비어있는 응답은 위임 디코더로 처리
     * - MleEnvelope.ENC_DATA_FIELD(`encData`) 필드가 없는 응답은 위임 디코더로 처리
     * @see MleConfig.mleDecoder
     * @param response Feign 응답 객체
     * @param type 디코딩할 대상 타입
     * @return 디코딩된 응답 객체
     */
    override fun decode(response: Response, type: Type): Any? {
        val body = response.body() ?: return delegate.decode(response, type)

        val bodyBytes = try {
            body.asInputStream().use { it.readBytes() }
        } catch (ex: Exception) {
            throw DecodeException(response.status(), "Failed to read response body", response.request(), ex)
        }

        if (bodyBytes.isEmpty()) {
            val rebuilt = response.toBuilder().body(bodyBytes.toString(), StandardCharsets.UTF_8).build()
            return delegate.decode(rebuilt, type)
        }

        val bodyString = String(bodyBytes, StandardCharsets.UTF_8)
        val containsEncData = bodyString.contains("\"${MleEnvelope.ENC_DATA_FIELD}\"")

        val decodedBodyString = if (containsEncData) {
            try {
                decryptor.decryptJsonPayload(bodyString)
            } catch (ex: MleException) {
                log.error(ex) { "[MleResponseDecoder] Failed to decrypt response" }
                throw DecodeException(
                    response.status(),
                    ex.message ?: "Failed to decrypt MLE response",
                    response.request(),
                    ex
                )
            }
        } else {
            bodyString
        }

        val rebuiltResponse = response.toBuilder()
            .body(decodedBodyString, StandardCharsets.UTF_8)
            .build()

        log.debug {
            "[MleResponseDecoder] ${response.request().httpMethod()} ${response.request().url()} - " +
                "decrypted: $containsEncData"
        }

        return delegate.decode(rebuiltResponse, type)
    }
}
