/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.Requirement

object JWSAlgorithms {
    @JvmStatic
    val DVS_P256_SHA256_HS256: JWSAlgorithm =
        JWSAlgorithm("DVS-P256-SHA256-HS256", Requirement.OPTIONAL)
}
