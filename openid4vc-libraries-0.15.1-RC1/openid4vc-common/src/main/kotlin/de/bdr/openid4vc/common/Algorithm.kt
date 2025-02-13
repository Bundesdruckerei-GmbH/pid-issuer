/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common

import COSE.AlgorithmID
import com.nimbusds.jose.JWSAlgorithm

enum class Algorithm(
    val jwsAlgorithm: JWSAlgorithm,
    val coseAlgorithm: AlgorithmID,
    val coseCurve: Int?
) {
    ES256(jwsAlgorithm = JWSAlgorithm.ES256, coseAlgorithm = AlgorithmID.ECDSA_256, coseCurve = 1),
    ES384(jwsAlgorithm = JWSAlgorithm.ES256, coseAlgorithm = AlgorithmID.ECDSA_384, coseCurve = 2),
    ES512(jwsAlgorithm = JWSAlgorithm.ES256, coseAlgorithm = AlgorithmID.ECDSA_512, coseCurve = 3),
    DVS_P256_SHA256_HS256(
        jwsAlgorithm = JWSAlgorithms.DVS_P256_SHA256_HS256,
        AlgorithmID.HMAC_SHA_256,
        coseCurve = null
    )
}
