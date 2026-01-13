package com.wiggleji.sslmle.config.mle

import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.crypto.RSADecrypter
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * MLE 복호화기
 * MLE 암호화된 데이터를 복호화
 */
class MleDecryptor(
    private val keyProvider: MleKeyProvider,
    private val objectMapper: ObjectMapper
) {

    /**
     * MLE 암호화된 문자열을 복호화하여 평문 문자열로 반환
     * @param encData 암호화된 데이터 문자열
     * @return 복호화된 평문 문자열
     */
    fun decryptToString(encData: String): String {
        try {
            val jweObject = JWEObject.parse(encData)
            jweObject.decrypt(RSADecrypter(keyProvider.clientPrivateKey()))
            val payload = jweObject.payload.toString()
            log.debug { "[MleDecryptor] Decrypted JWE with kid=${jweObject.header.keyID}" }
            return payload
        } catch (ex: Exception) {
            throw MleException("Failed to decrypt MLE payload", ex)
        }
    }

    /**
     * MLE 암호화된 JSON 페이로드를 복호화하여 평문 JSON 문자열로 반환
     * @see MleEnvelope.ENC_DATA_FIELD
     * @param encryptedJson 암호화된 JSON 문자열
     * @return 복호화된 평문 JSON 문자열
     */
    fun decryptJsonPayload(encryptedJson: String): String {
        val node = objectMapper.readTree(encryptedJson)
        val encDataNode = node.get(MleEnvelope.ENC_DATA_FIELD)
            ?: throw MleException("Encrypted payload missing '${MleEnvelope.ENC_DATA_FIELD}' field")
        return decryptToString(encDataNode.asText())
    }
}
