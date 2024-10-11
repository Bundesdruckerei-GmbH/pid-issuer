/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.signing

import de.bdr.openid4vc.common.Algorithm

interface Signer {
    val algorithm: Algorithm
    val keys: KeyMaterial

    fun sign(data: ByteArray): ByteArray
}
