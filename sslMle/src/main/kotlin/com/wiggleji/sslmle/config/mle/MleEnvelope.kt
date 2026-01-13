package com.wiggleji.sslmle.config.mle

import com.fasterxml.jackson.databind.ObjectMapper

/**
 * MLE 암호화된 데이터 래퍼
 * encData 필드에 암호화된 문자열을 포함
 */
data class MleEnvelope(
    val encData: String
) {
    fun asMap(): Map<String, String> = mapOf(ENC_DATA_FIELD to encData)

    fun asJson(objectMapper: ObjectMapper): String = objectMapper.writeValueAsString(asMap())

    companion object {
        // MLE 암호화 body field
        const val ENC_DATA_FIELD = "encData"
    }
}
