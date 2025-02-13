/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.signing

import com.nimbusds.jose.jwk.JWK

sealed interface KeyMaterial {
    val jwk: JWK
}
