package com.wiggleji.sslmle.config.mle

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * MLE 설정 프로퍼티
 * @property enabled MLE 사용 여부
 * @property keyId JWE 키 ID
 * @property keyIdHeaderName JWE 키 ID 헤더 이름
 * @property serverCertificateLocation 서버 인증서 위치
 * @property clientPrivateKeyLocation 클라이언트 개인 키 위치
 * @property clientPrivateKeyPassphrase 클라이언트 개인 키 암호 구문
 * @property defaultContentType 기본 콘텐츠 타입
 */
@ConfigurationProperties(prefix = "mle")
data class MleProperties(
    val enabled: Boolean = false,
    val keyId: String? = null,
    val keyIdHeaderName: String = "keyId",
    val serverCertificateLocation: String? = null,
    val clientPrivateKeyLocation: String? = null,
    val clientPrivateKeyPassphrase: String? = null,
    val defaultContentType: String = "application/json"
)
