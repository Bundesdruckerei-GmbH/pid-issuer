/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vci

import java.time.Duration

/** A service that provides nonce values used for DPoP. */
interface NonceService {

    /**
     * Generates a new nonce. Nonce values must be generated with enough entropy to be secure. This
     * means it must not be possible for a third party to generate or guess valid nonces.
     */
    fun generate(purpose: NoncePurpose): NonceAndValidityDuration

    /**
     * Validates if the given nonce is a valid nonce, that was previously generated, and that is not
     * expired.
     *
     * @throws IllegalArgumentException if the nonce is not valid or expired
     */
    fun validate(nonce: String, expectedPurpose: NoncePurpose)

    data class NonceAndValidityDuration(val nonce: String, val validity: Duration?)

    /**
     * A purpose that is used to generated nonces in a specific context. The purpose is provided on
     * nonce generation and must match the expected purpose on validation. This allows to use the
     * same service for different situations without interchangable nonces.
     *
     * @see StandardNoncePurpose
     */
    interface NoncePurpose {
        val bytes: ByteArray
    }

    enum class StandardNoncePurpose(override val bytes: ByteArray) : NoncePurpose {
        DPOP(byteArrayOf(0)),
        ATTESTATION(byteArrayOf(1)),
        C_NONCE(byteArrayOf(2)),
    }
}
