/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.credentials

import com.nimbusds.jose.jwk.JWK
import java.security.cert.X509Certificate

data class IssuerInfo(
    val identifier: String,
    val x509Certificate: X509Certificate? = null,
    val jwk: JWK? = null,
)
