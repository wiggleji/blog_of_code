package com.wiggleji.sslmle.config.ssl

import mu.KotlinLogging
import org.springframework.core.io.ResourceLoader
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

private val log = KotlinLogging.logger {}

class SslContextFactory(
    private val resourceLoader: ResourceLoader
) {

    fun createSslContext(properties: SslProperties): SSLContext {
        val keyStore = loadKeyStore(
            location = properties.keyStore.location,
            password = properties.keyStore.password,
            type = properties.keyStore.type,
            description = "keyStore"
        )

        val trustStore = loadKeyStore(
            location = properties.trustStore.location,
            password = properties.trustStore.password,
            type = properties.trustStore.type,
            description = "trustStore"
        )

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, properties.keyStore.password.toCharArray())
        }

        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(trustStore)
        }

        return SSLContext.getInstance("TLS").apply {
            init(kmf.keyManagers, tmf.trustManagers, SecureRandom())
        }
    }

    private fun loadKeyStore(
        location: String?,
        password: String,
        type: String,
        description: String
    ): KeyStore {
        if (location.isNullOrBlank()) {
            error("[$description] location must be provided when SSL is enabled")
        }

        val resource = resourceLoader.getResource(location)
        if (!resource.exists()) {
            error("[$description] Resource not found: $location")
        }

        return KeyStore.getInstance(type).apply {
            resource.inputStream.use { load(it, password.toCharArray()) }
        }.also {
            log.debug { "[SslContextFactory] Loaded $description from $location" }
        }
    }
}
