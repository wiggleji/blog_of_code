package com.wiggleji.sslmle.config.mle

import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.RSAEncrypter
import mu.KotlinLogging
import java.nio.charset.StandardCharsets

private val log = KotlinLogging.logger {}

/**
 * MLE 암호화기
 * 데이터를 MLE 규격에 맞게 암호화 (RSA-OAEP-256 / AES-GCM)
 */
class MleEncryptor(
    private val properties: MleProperties,
    private val keyProvider: MleKeyProvider,
    private val objectMapper: ObjectMapper
) {

    /**
     * 주어진 페이로드를 MLE 규격에 맞게 암호화하여 MleEnvelope로 반환
     * @see MleEnvelope.ENC_DATA_FIELD
     * @param payload 암호화할 페이로드 (String, ByteArray, 또는 기타 객체)
     * @param contentType 페이로드의 콘텐츠 타입 (기본값: properties.defaultContentType)
     * @param keyIdOverride JWE 키 ID를 오버라이드할 경우 지정 (기본값: null)
     * @return 암호화된 MleEnvelope
     */
    fun encrypt(payload: Any?, contentType: String? = null, keyIdOverride: String? = null): MleEnvelope {
        val plainText = when (payload) {
            null -> ""
            is String -> payload
            is ByteArray -> String(payload, StandardCharsets.UTF_8)
            else -> objectMapper.writeValueAsString(payload)
        }

        val keyId = keyIdOverride ?: properties.keyId
            ?: error("[MleEncryptor] mle.key-id must be provided when MLE is enabled")

        val headerBuilder = JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A128GCM)
            .keyID(keyId)
            .customParam("iat", System.currentTimeMillis())

        val resolvedContentType = (contentType ?: properties.defaultContentType).trim()
        if (resolvedContentType.isNotEmpty() && !resolvedContentType.equals("application/json", ignoreCase = true)) {
            headerBuilder.contentType(resolvedContentType)
        }

        val header = headerBuilder.build()
        val jweObject = JWEObject(header, Payload(plainText))
        jweObject.encrypt(RSAEncrypter(keyProvider.serverPublicKey()))

        val serialized = jweObject.serialize()
        log.debug { "[MleEncryptor] Created JWE with kid=${header.keyID}" }
        return MleEnvelope(serialized)
    }

    /**
     * 주어진 페이로드를 MLE 규격에 맞게 암호화하여 JSON 문자열로 반환
     * @param payload 암호화할 페이로드 (String, ByteArray, 또는 기타 객체)
     * @param contentType 페이로드의 콘텐츠 타입 (기본값: properties.defaultContentType)
     * @param keyIdOverride JWE 키 ID를 오버라이드할 경우 지정 (기본값: null)
     * @return 암호화된 MLE 페이로드의 JSON 문자열
     */
    fun encryptToJson(payload: Any?, contentType: String? = null, keyIdOverride: String? = null): String =
        encrypt(payload, contentType, keyIdOverride).asJson(objectMapper)
}
