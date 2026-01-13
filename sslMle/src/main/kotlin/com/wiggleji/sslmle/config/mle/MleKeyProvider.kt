package com.wiggleji.sslmle.config.mle

import mu.KotlinLogging
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMEncryptedKeyPair
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo
import org.springframework.core.io.ResourceLoader
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.security.PrivateKey
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.atomic.AtomicReference

private val log = KotlinLogging.logger {}

/**
 * MLE 키 제공자
 * MLE 서버 공개 키 및 클라이언트 개인 키를 로드하고 캐싱
 */
class MleKeyProvider(
    private val properties: MleProperties,
    private val resourceLoader: ResourceLoader
) {
    // CAS 방식의 키 캐싱을 위한 AtomicReference
    private val publicKeyRef = AtomicReference<RSAPublicKey?>()
    private val privateKeyRef = AtomicReference<PrivateKey?>()

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    /**
     * MLE 서버 공개 키 로드 및 캐싱
     * @return RSAPublicKey
     */
    fun serverPublicKey(): RSAPublicKey {
        publicKeyRef.get()?.let { return it }

        val loaded = loadServerPublicKey()
        publicKeyRef.compareAndSet(null, loaded)
        return publicKeyRef.get() ?: loaded
    }

    /**
     * MLE 클라이언트 개인 키 로드 및 캐싱
     * @return PrivateKey
     */
    fun clientPrivateKey(): PrivateKey {
        privateKeyRef.get()?.let { return it }

        val loaded = loadClientPrivateKey()
        privateKeyRef.compareAndSet(null, loaded)
        return privateKeyRef.get() ?: loaded
    }

    /**
     * MLE 서버 공개 키를 X.509 인증서(CertificateFactory)에서 로드
     * @return RSAPublicKey
     */
    private fun loadServerPublicKey(): RSAPublicKey {
        val location = properties.serverCertificateLocation
            ?: error("[MleKeyProvider] mle.server-certificate-location must be provided")

        val resource = resourceLoader.getResource(location)
        val certBytes = resource.inputStream.use { it.readBytes() }

        ByteArrayInputStream(certBytes).use { input ->
            val cert = CertificateFactory.getInstance("X.509").generateCertificate(input)
            val publicKey = cert.publicKey

            if (publicKey !is RSAPublicKey) {
                error("[MleKeyProvider] Server certificate does not contain an RSA public key")
            }

            log.debug { "[MleKeyProvider] Loaded server public key from $location" }
            return publicKey
        }
    }

    /**
     * MLE 클라이언트 개인 키를 PEM 형식에서 로드
     * @return PrivateKey
     */
    private fun loadClientPrivateKey(): PrivateKey {
        val location = properties.clientPrivateKeyLocation
            ?: error("[MleKeyProvider] mle.client-private-key-location must be provided")

        val resource = resourceLoader.getResource(location)
        val keyBytes = resource.inputStream.use { it.readBytes() }

        ByteArrayInputStream(keyBytes).use { input ->
            InputStreamReader(input, StandardCharsets.UTF_8).use { isr ->
                BufferedReader(isr).use { reader ->
                    PEMParser(reader).use { parser ->
                        val converter = JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        val parsed = parser.readObject()
                            ?: error("[MleKeyProvider] Unable to parse private key from $location")

                        val privateKey = when (parsed) {
                            is PEMEncryptedKeyPair -> {
                                val passphrase = properties.clientPrivateKeyPassphrase
                                    ?: error("[MleKeyProvider] Encrypted key requires mle.client-private-key-passphrase")
                                val decryptor = JcePEMDecryptorProviderBuilder().build(passphrase.toCharArray())
                                converter.getKeyPair(parsed.decryptKeyPair(decryptor)).private
                            }
                            is PEMKeyPair -> converter.getKeyPair(parsed).private
                            is PKCS8EncryptedPrivateKeyInfo -> {
                                val passphrase = properties.clientPrivateKeyPassphrase
                                    ?: error("[MleKeyProvider] Encrypted key requires mle.client-private-key-passphrase")
                                val decryptor = JceOpenSSLPKCS8DecryptorProviderBuilder().build(passphrase.toCharArray())
                                converter.getPrivateKey(parsed.decryptPrivateKeyInfo(decryptor))
                            }
                            is PrivateKeyInfo -> converter.getPrivateKey(parsed)
                            is PrivateKey -> parsed
                            else -> error("[MleKeyProvider] Unsupported private key format: ${parsed.javaClass.name}")
                        }

                        log.debug { "[MleKeyProvider] Loaded client private key from $location" }
                        return privateKey
                    }
                }
            }
        }
    }
}
