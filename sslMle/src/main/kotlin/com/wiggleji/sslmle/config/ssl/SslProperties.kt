package com.wiggleji.sslmle.config.ssl

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ssl")
data class SslProperties(
    val enabled: Boolean = false,
    val keyStore: KeyStoreProperties = KeyStoreProperties(),
    val trustStore: KeyStoreProperties = KeyStoreProperties()
)

data class KeyStoreProperties(
    val location: String? = null,
    val password: String = "",
    val type: String = "PKCS12"
)
