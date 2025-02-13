/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.statuslist

import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jwt.SignedJWT

fun interface Verifier {
    /**
     * This method produces a JWSVerifier based on a given SignedJWT (containing the status list
     * token payload).
     *
     * This approach allows to respect values set in the header and claims to construct the
     * JWSVerifier and thus allows to implement the necessary verification steps demanded by several
     * specifications.
     *
     * @return a JWSVerifier that will be used to verify the status list token signature.
     */
    fun verifier(jwt: SignedJWT): JWSVerifier?
}
