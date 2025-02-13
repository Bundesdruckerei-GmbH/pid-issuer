/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.signing

import com.nimbusds.jose.jwk.Curve
import java.io.InputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.security.interfaces.ECKey

class Pkcs12Signer(keystore: InputStream, password: String) : JcaKeyBasedSigner() {

    override val privateKey: PrivateKey

    private val chain: List<X509Certificate>

    init {
        val ks = KeyStore.getInstance("pkcs12")
        ks.load(keystore, password.toCharArray())
        val aliases = ks.aliases().toList()
        require(aliases.size == 1) { "Expected a keystore with a key entry" }
        privateKey =
            ks.getKey(aliases.first(), password.toCharArray()) as PrivateKey?
                ?: throw IllegalArgumentException("Expected a keystore with a key entry")
        chain =
            ks.getCertificateChain(aliases.first())?.map { it as X509Certificate }
                ?: throw IllegalArgumentException("Expected a keystore with a certificate chain")
    }

    override val keys = X509KeyMaterial(chain)

    override val supportedAlgorithm = algorithm(chain[0])

    private fun algorithm(cert: X509Certificate): SupportedAlgorithm {
        val curve = Curve.forECParameterSpec((cert.publicKey as ECKey).params)
        return SupportedAlgorithm.entries.firstOrNull { it.curve == curve }
            ?: throw IllegalArgumentException("Unsupported curve $curve")
    }
}
