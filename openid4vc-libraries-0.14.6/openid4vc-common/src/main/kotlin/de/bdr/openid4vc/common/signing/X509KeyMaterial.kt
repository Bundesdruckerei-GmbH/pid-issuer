/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.signing

import com.nimbusds.jose.jwk.JWK
import java.security.cert.X509Certificate
import java.util.Base64
import org.bouncycastle.asn1.x509.Certificate
import org.bouncycastle.asn1.x509.IssuerSerial

class X509KeyMaterial(val certificates: List<X509Certificate>) : KeyMaterial {

    override val jwk =
        JWK.parse(
            JWK.parse(certificates.first()).toJSONObject().apply {
                set("kid", kid(certificates.first()))
            }
        )

    private fun kid(first: X509Certificate): String {
        val cert = Certificate.getInstance(first.encoded)
        return Base64.getEncoder()
            .encodeToString(IssuerSerial(cert.issuer, cert.serialNumber.value).encoded)
    }
}
