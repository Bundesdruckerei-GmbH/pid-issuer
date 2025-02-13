/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.auth

import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBORType
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable
import java.io.ByteArrayInputStream
import java.security.cert.CertPath
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * A [CBORable][ICBORable] [List] representation of the 'x5chain' header attribute to be used with [COSE.SignMessage]
 * or [COSE.Sign1Message].
 *
 * See RFC 9360 for full documentation of the 'x5chain' header.
 */
sealed interface X5Chain : ICBORable, List<X509Certificate> {

    /**
     * The certificate containing the public part of the signing key the [COSE.SignMessage] or [COSE.Sign1Message]
     * is or will be signed with.
     *
     * This is always the first certificate in the chain.
     */
    val endEntityCert: X509Certificate

    private data class Single(override val endEntityCert: X509Certificate) :
        X5Chain,
        List<X509Certificate> by listOf(endEntityCert) {
        override fun asCBOR(): CBORObject = CBORObject.FromObject(endEntityCert.encoded)
        override fun toString() = "X5Chain(certificates=[$endEntityCert])"
    }

    private data class Multiple(private val certificates: List<X509Certificate>) :
        X5Chain,
        List<X509Certificate> by certificates {
        override val endEntityCert = certificates[0]

        override fun asCBOR(): CBORObject = CBORObject.NewArray().apply {
            certificates.forEach {
                Add(CBORObject.FromObject(it.encoded))
            }
        }

        override fun toString() = "X5Chain(certificates=$certificates)"
    }

    companion object {

        fun fromCBOR(cborObject: CBORObject): X5Chain {
            val cf = CertificateFactory.getInstance("X509")

            return when (cborObject.type) {
                CBORType.ByteString -> Single(decode(cf, cborObject))

                CBORType.Array -> {
                    val values = cborObject.values
                    if (values.isNullOrEmpty()) throw notFoundException(cborObject)
                    require(values.size >= 2) {
                        "Single X.509 Certificates must be provided as a single BytesString, not as Array."
                    }
                    Multiple(values.map { decode(cf, it) })
                }

                else -> throw notFoundException(cborObject)
            }
        }
        @Suppress("unused")
        fun fromCertPath(certPath: CertPath): X5Chain = when (certPath.certificates.size) {
            0 -> throw notFoundException(certPath)
            1 -> Single(
                certPath.certificates[0] as? X509Certificate
                    ?: throw notFoundException(certPath)
            )

            else -> Multiple(
                certPath.certificates.mapIndexed { index, certificate ->
                    requireNotNull(certificate as? X509Certificate) {
                        "The Certificate at index $index was not an instance of ${X509Certificate::class.simpleName}."
                    }
                }
            )
        }


        @Suppress("unused")
        fun of(certificate1: X509Certificate): X5Chain = Single(certificate1)

        @Suppress("unused")
        fun of(
            certificate1: X509Certificate,
            certificate2: X509Certificate,
            vararg moreCertificates: X509Certificate
        ): X5Chain = Multiple(listOf(certificate1, certificate2, *moreCertificates))

        private fun decode(cf: CertificateFactory, cborObject: CBORObject): X509Certificate {
            return try {
                ByteArrayInputStream(cborObject.GetByteString()).use {
                    cf.generateCertificate(it) as X509Certificate
                }
            } catch (t: Throwable) {
                throw notFoundException(cborObject, t)
            }
        }

        private fun notFoundException(obj: Any, cause: Throwable? = null) = IllegalArgumentException(
            "Object `$obj` does not contain an ${X509Certificate::class.simpleName}",
            cause
        )
    }
}


fun X5Chain.toCertPath(): CertPath = CertificateFactory.getInstance("X509").generateCertPath(this)
