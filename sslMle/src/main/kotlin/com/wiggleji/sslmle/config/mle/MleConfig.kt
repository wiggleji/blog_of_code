package com.wiggleji.sslmle.config.mle

import com.fasterxml.jackson.databind.ObjectMapper
import feign.RequestInterceptor
import org.springframework.beans.factory.annotation.Autowired
import feign.codec.Decoder
import mu.KotlinLogging
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder
import org.springframework.cloud.openfeign.support.SpringDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader

private val log = KotlinLogging.logger {}

@Configuration
@ConditionalOnProperty(name = ["mle.enabled"], havingValue = "true")
@EnableConfigurationProperties(MleProperties::class)
class MleConfig(
    private val messageConverters: ObjectProvider<FeignHttpMessageConverters>,
    private val resourceLoader: ResourceLoader,
    @Autowired private val objectMapper: ObjectMapper
) {

    @Bean
    fun mleKeyProvider(properties: MleProperties): MleKeyProvider {
        log.info { "[MleConfig] Initializing MLE key provider" }
        return MleKeyProvider(properties, resourceLoader)
    }

    @Bean
    fun mleEncryptor(properties: MleProperties, keyProvider: MleKeyProvider): MleEncryptor {
        return MleEncryptor(properties, keyProvider, objectMapper)
    }

    @Bean
    fun mleDecryptor(keyProvider: MleKeyProvider): MleDecryptor {
        return MleDecryptor(keyProvider, objectMapper)
    }

    /**
     * MLE 요청 Interceptor 빈
     * HTTP 요청 시 MLE 암호화 수행
     */
    @Bean
    fun mleRequestInterceptor(encryptor: MleEncryptor, properties: MleProperties): RequestInterceptor {
        log.info { "[MleConfig] Registering MLE request interceptor" }
        return MleRequestInterceptor(encryptor, properties)
    }

    /**
     * MLE 응답 Decoder 빈
     * HTTP 응답 시 MLE 복호화 수행
     */
    @Bean
    fun mleDecoder(decryptor: MleDecryptor): Decoder {
        val delegate = ResponseEntityDecoder(SpringDecoder(messageConverters))
        log.info { "[MleConfig] Registering MLE response decoder" }
        return MleResponseDecoder(delegate, decryptor)
    }
}
