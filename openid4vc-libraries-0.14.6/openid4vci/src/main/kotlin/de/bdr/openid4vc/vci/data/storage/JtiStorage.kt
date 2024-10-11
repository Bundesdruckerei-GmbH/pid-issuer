/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.vci.data.storage

import java.time.Instant

/**
 * For DPoP the JTI is used to check that a single DPoP proof is only used once. This service stores
 * JTI values and checks if they are unused.
 */
fun interface JtiStorage {
    /**
     * Checks if the given jti value has been used in the past.
     *
     * Future invocations of this method with the same jti must return `false` until at least
     * `validUntil`. Invocations after that instant may return `true` again.
     *
     * @return `true` if the jti value is unused, `false` otherwise
     */
    fun isUnused(jti: String, validUntil: Instant): Boolean
}
