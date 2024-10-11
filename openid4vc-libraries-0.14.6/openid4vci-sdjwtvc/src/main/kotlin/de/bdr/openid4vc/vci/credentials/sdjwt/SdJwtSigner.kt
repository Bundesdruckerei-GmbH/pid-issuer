/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.vci.credentials.sdjwt

import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.util.Base64
import com.nimbusds.jose.util.Base64URL
import de.bdr.openid4vc.common.signing.JwkKeyMaterial
import de.bdr.openid4vc.common.signing.Signer
import de.bdr.openid4vc.common.signing.SignerToJwsSignerAdapter
import de.bdr.openid4vc.common.signing.X509KeyMaterial
import java.security.MessageDigest
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import org.bouncycastle.asn1.x509.Certificate
import org.bouncycastle.asn1.x509.IssuerSerial

internal class SdJwtSigner(signer: Signer, private val jadesSignatures: Boolean = false) {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyy-MM-dd'T'hh:mm:ss'Z'")

    private val jwsSigner = SignerToJwsSignerAdapter(signer)

    private val certChain =
        when (signer.keys) {
            is X509KeyMaterial -> (signer.keys as X509KeyMaterial).certificates
            else -> null
        }

    init {
        check(certChain?.isNotEmpty() ?: true) { "Signer must not return an empty cert chain" }
    }

    private val encodedCertChain = certChain?.map { it.encoded }

    private val kid =
        when (signer.keys) {
            is X509KeyMaterial -> {
                val cert = Certificate.getInstance(encodedCertChain!![0])
                java.util.Base64.getEncoder()
                    .encodeToString(IssuerSerial(cert.issuer, cert.serialNumber.value).encoded)
            }
            is JwkKeyMaterial -> {
                (signer.keys as JwkKeyMaterial).jwk.keyID
            }
        }

    fun customizeHeader(builder: JWSHeader.Builder) {
        header(builder)
    }

    fun header(
        builder: JWSHeader.Builder = JWSHeader.Builder(jwsSigner.jwsAlgorithm)
    ): JWSHeader.Builder {

        if (encodedCertChain != null) {
            builder.x509CertChain(encodedCertChain.map { Base64.encode(it) })
        }

        builder.keyID(kid)

        if (jadesSignatures) {

            check(certChain != null) {
                "Jades signatures require a signer that provides X509KeyMaterial"
            }

            builder.x509CertSHA256Thumbprint(
                Base64URL.encode(
                    MessageDigest.getInstance("SHA-256").digest(encodedCertChain!![0]!!)
                )
            )

            builder.criticalParams(setOf("sigT"))
            builder.customParam("sigT", dateTimeFormatter.format(ZonedDateTime.now(ZoneId.of("Z"))))
        }

        return builder
    }

    fun signer() = jwsSigner
}
