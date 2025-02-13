/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package tests

import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.security.cert.X509Certificate
import java.security.spec.AlgorithmParameterSpec
import java.time.LocalDate
import java.time.LocalTime.MIDNIGHT
import java.time.ZoneOffset.UTC
import java.util.Date
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

class TestCa(val algorithm: Algorithm) {

    val private: PrivateKey
    val public: PublicKey
    val cert: X509Certificate

    private val issuer = X500Name("CN=test-ca")

    private val signer: ContentSigner

    private var serial = BigInteger.ONE

    init {
        Security.addProvider(BouncyCastleProvider())
        val keyGen = KeyPairGenerator.getInstance(algorithm.keyAlgorithm, "BC")
        keyGen.initialize(algorithm.keyParamSpec)
        val keyPair = keyGen.genKeyPair()
        private = keyPair.private
        public = keyPair.public

        signer = JcaContentSignerBuilder("SHA256WithECDSA").build(private)

        val certHolder =
            JcaX509v3CertificateBuilder(
                    issuer,
                    BigInteger.ZERO,
                    LocalDate.now().dateAtStartOfDay(),
                    LocalDate.now().plusYears(1).dateAtStartOfDay(),
                    issuer,
                    public
                )
                .addExtension(
                    Extension.create(Extension.basicConstraints, true, BasicConstraints(true))
                )
                .build(signer)
        cert = JcaX509CertificateConverter().getCertificate(certHolder)
    }

    fun generate(certAlgorithm: Algorithm, cn: String): Pair<X509Certificate, KeyPair> {
        val keyGen = KeyPairGenerator.getInstance(certAlgorithm.keyAlgorithm, "BC")
        keyGen.initialize(certAlgorithm.keyParamSpec)
        val keyPair = keyGen.genKeyPair()
        val certHolder =
            JcaX509v3CertificateBuilder(
                    issuer,
                    BigInteger.ZERO,
                    LocalDate.now().dateAtStartOfDay(),
                    LocalDate.now().plusYears(1).dateAtStartOfDay(),
                    X500Name("CN=$cn"),
                    keyPair.public
                )
                .addExtension(
                    Extension.create(Extension.basicConstraints, true, BasicConstraints(false))
                )
                .build(signer)
        return Pair(JcaX509CertificateConverter().getCertificate(certHolder), keyPair)
    }

    enum class Algorithm(
        val signingJcaAlgorithm: String,
        val keyAlgorithm: String,
        val keyParamSpec: AlgorithmParameterSpec
    ) {
        SHA256WithECDSA("SHA256WithECDSA", "ECDSA", ECNamedCurveTable.getParameterSpec("P-256")),
        SHA384WithECDSA("SHA384WithECDSA", "ECDSA", ECNamedCurveTable.getParameterSpec("P-384")),
        SHA512WithECDSA("SHA512WithECDSA", "ECDSA", ECNamedCurveTable.getParameterSpec("P-521"))
    }
}

fun LocalDate.dateAtStartOfDay() = Date(toEpochSecond(MIDNIGHT, UTC) * 1000)
