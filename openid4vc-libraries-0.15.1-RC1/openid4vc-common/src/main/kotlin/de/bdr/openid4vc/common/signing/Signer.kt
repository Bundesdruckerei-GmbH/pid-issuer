/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.signing

import de.bdr.openid4vc.common.Algorithm

interface Signer {
    val algorithm: Algorithm
    val keys: KeyMaterial

    fun sign(data: ByteArray): ByteArray
}
