package com.wiggleji.sslmle.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "http.client.pool")
data class HttpClientProperties(
    val maxTotal: Int = 100,
    val maxPerRoute: Int = 20
)
