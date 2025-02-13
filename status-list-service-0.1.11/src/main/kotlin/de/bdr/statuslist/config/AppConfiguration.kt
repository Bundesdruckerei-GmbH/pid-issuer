/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.config

import COSE.Sign1Message
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.util.Base64
import com.upokecenter.cbor.CBORObject
import de.bdr.openid4vc.common.signing.Pkcs12Signer
import de.bdr.openid4vc.common.signing.Signer
import de.bdr.statuslist.config.OnUnderflowBehavior.FAIL
import de.bdr.statuslist.service.StorageFiles
import de.bdr.statuslist.util.sha256
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import org.bouncycastle.asn1.x509.Certificate
import org.bouncycastle.asn1.x509.IssuerSerial
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.DefaultResourceLoader

@ConfigurationProperties("app")
class AppConfiguration(
    val publicUrl: String,
    storageDirectory: String,
    val cacheDuration: Duration,
    val statusListPools: Map<String, StatusListPoolConfiguration>,
    val redis: RedisConnectionConfiguration = RedisConnectionConfiguration(),
) {
    companion object {
        private val POOL_ID_REGEX = Regex("[a-z0-9_-]+")
    }

    val storageDirectory: Path = Path.of(storageDirectory)

    init {
        StorageFiles.statusListTokenDirectories(this@AppConfiguration).forEach {
            Files.createDirectories(it)
        }
        statusListPools.keys.forEach { check(it.matches(POOL_ID_REGEX)) { "Invalid pool id $it" } }
    }
}

class SignerConfiguration(
    val keystore: String,
    val password: String,
    val header: HeaderFeatures = HeaderFeatures(),
)

class HeaderFeatures(val x5c: Boolean = true, val kid: Boolean = true, val jwk: Boolean = false)

class StatusListPoolConfiguration(
    apiKey: String? = null,
    apiKeys: List<String>? = null,
    val size: Int,
    val bits: Int,
    val issuer: String,
    val precreation: PrecreationConfiguration,
    val prefetch: PrefetchConfiguration,
    val updateInterval: Duration,
    val listLifetime: Duration,
    val aggregationId: String?,
    private val signer: SignerConfiguration,
) {

    companion object {
        private val CWT_HEADER_KID = CBORObject.FromObject(4)
        private val CWT_HEADER_X5C = CBORObject.FromObject(33)
    }

    val apiKeyHashes =
        mutableListOf<String>()
            .apply {
                apiKeys?.let { addAll(it) }
                apiKey?.let { add(it) }
            }
            .map { sha256(it.toByteArray()) }

    val signerInstance: Signer

    private val encodedCertChain: List<ByteArray>

    private val kid: String

    init {
        check(updateInterval.isPositive) { "update-interval must be positive" }

        check(listLifetime.isPositive) { "list-lifetime must be positive" }
        check(listLifetime > updateInterval) {
            "list-lifetime must be greater than update-interval"
        }
        check(bits == 1 || bits == 2 || bits == 4 || bits == 8) {
            "The allowed values for bits are 1, 2, 4 and 8."
        }
        check(size > 0 && (size * bits) % 8 == 0) {
            "The size must be greater than 0 and (size * bits) % 8 must be 0"
        }
        check(size >= prefetch.capacity) { "Size must be >= prefetch.capacity" }

        val apiKeyValuesSet = listOf(apiKey, apiKeys).count { it != null }
        check(apiKeyValuesSet == 1) { "either api-key or api-keys must be set" }
        check(apiKeys?.isNotEmpty() ?: true) { "api-keys must not be empty" }

        this.signerInstance =
            Pkcs12Signer(
                DefaultResourceLoader().getResource(signer.keystore).inputStream,
                signer.password,
            )
        this.encodedCertChain = signerInstance.keys.certificates.map { it.encoded }
        this.kid =
            this.encodedCertChain[0].let { encoded ->
                val cert = Certificate.getInstance(encoded)
                java.util.Base64.getEncoder()
                    .encodeToString(IssuerSerial(cert.issuer, cert.serialNumber.value).encoded)
            }
    }

    fun modifyJwsHeader(builder: JWSHeader.Builder) {
        if (signer.header.x5c) {
            builder.x509CertChain(encodedCertChain.map { Base64.encode(it) })
        }
        if (signer.header.kid) {
            builder.keyID(kid)
        }
        if (signer.header.jwk) {
            builder.jwk(signerInstance.keys.jwk)
        }
    }

    fun modifyCwtHeader(sign1Message: Sign1Message) {
        if (signer.header.x5c || signer.header.jwk) {
            sign1Message.protectedAttributes[CWT_HEADER_X5C] =
                if (encodedCertChain.size == 1) {
                    CBORObject.FromObject(encodedCertChain[0])
                } else {
                    val array = CBORObject.NewArray()
                    encodedCertChain.forEachIndexed { _, bytes ->
                        array.Add(CBORObject.FromObject(bytes))
                    }
                    array
                }
        }

        if (signer.header.kid) {
            sign1Message.protectedAttributes[CWT_HEADER_KID] =
                CBORObject.FromObject(kid.toByteArray())
        }
    }
}

class PrecreationConfiguration(val checkDelay: Duration, val lists: Int) {
    init {
        check(checkDelay.isPositive) { "precreation.check-delay must be positive" }
        check(lists >= 1) { "precreation.lists must be >= 1" }
    }
}

class PrefetchConfiguration(
    val threshold: Int,
    val capacity: Int,
    val onUnderflow: OnUnderflowBehavior = FAIL,
) {
    init {
        check(threshold <= capacity) { "Prefetch threshold must be <= capacity" }
    }
}

enum class OnUnderflowBehavior {
    DELAY,
    FAIL,
}

class RedisConnectionConfiguration(
    val host: String = "localhost",
    val port: Int = 6379,
    val persistenceStrategy: PersistenceStrategy = PersistenceStrategy.APPEND_FSYNC_ALWAYS,
)

enum class PersistenceStrategy {
    APPEND_FSYNC_ALWAYS,
    DISABLED,
}
