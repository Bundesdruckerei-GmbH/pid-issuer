/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.vci.service.endpoints

/** A service that provides nonce values used for DPoP. */
interface NonceService {

    /**
     * Generates a new nonce. Nonce values must be generated with enough entropy to be secure. This
     * means it must not be possible for a third party to generate or guess valid nonces.
     */
    fun generate(): String

    /**
     * Validates if the given nonce is a valid nonce, that was previously generated, and that is not
     * expired.
     *
     * @throws IllegalArgumentException if the nonce is not valid or expired
     */
    fun validate(nonce: String)
}
