/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vci

import de.bdr.openid4vc.common.DurationToSecondsSerializer
import java.time.Duration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NonceResponse(
    @SerialName("c_nonce") val cNonce: String,
    @SerialName("c_nonce_expires_in")
    @Serializable(with = DurationToSecondsSerializer::class)
    val cNonceExpiresIn: Duration? = null,
)
