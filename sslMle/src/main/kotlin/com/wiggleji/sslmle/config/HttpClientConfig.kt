package com.wiggleji.sslmle.config

import com.wiggleji.sslmle.config.ssl.SslContextFactory
import com.wiggleji.sslmle.config.ssl.SslProperties
import feign.Client
import feign.hc5.ApacheHttp5Client
import mu.KotlinLogging
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader

private val log = KotlinLogging.logger {}

@Configuration
@EnableConfigurationProperties(HttpClientProperties::class, SslProperties::class)
class HttpClientConfig(
    private val resourceLoader: ResourceLoader
) {

    @Bean
    fun feignClient(
        httpClientProperties: HttpClientProperties,
        sslProperties: SslProperties
    ): Client {
        val connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(httpClientProperties.maxTotal)
            .setMaxConnPerRoute(httpClientProperties.maxPerRoute)

        if (sslProperties.enabled) {
            val sslContextFactory = SslContextFactory(resourceLoader)
            val sslContext = sslContextFactory.createSslContext(sslProperties)
            val tlsStrategy = DefaultClientTlsStrategy(sslContext)
            connectionManagerBuilder.setTlsSocketStrategy(tlsStrategy)
            log.info { "[HttpClientConfig] SSL/mTLS enabled" }
        }

        val connectionManager = connectionManagerBuilder.build()

        val httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .evictExpiredConnections()
            .build()

        log.info {
            "[HttpClientConfig] Initialized Apache HttpClient5 - " +
                "maxTotal: ${httpClientProperties.maxTotal}, " +
                "maxPerRoute: ${httpClientProperties.maxPerRoute}, " +
                "ssl: ${sslProperties.enabled}"
        }

        return ApacheHttp5Client(httpClient)
    }
}
