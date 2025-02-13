/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.Requirement

object JWSAlgorithms {
    @JvmStatic
    val DVS_P256_SHA256_HS256: JWSAlgorithm =
        JWSAlgorithm("DVS-P256-SHA256-HS256", Requirement.OPTIONAL)
}
