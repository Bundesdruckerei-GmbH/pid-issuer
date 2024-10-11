/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.vci.service.dpop

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.SignedJWT
import de.bdr.openid4vc.vci.service.HttpRequest
import de.bdr.openid4vc.vci.utils.jwsVerifierForKey

/** Represents a DPoP proof according to RFC-9449. */
class DPoPProof(val jwt: SignedJWT) {

    companion object {

        const val DPOP_TYP = "dpop+jwt"

        /** Parses a potential DPoP proof from the dpop header of an `HttpRequest`. */
        fun parse(request: HttpRequest<*>): DPoPProof? {
            val headerCount = request.headers.headers["dpop"]?.size ?: 0
            return if (headerCount > 1) {
                error("Multiple dpop headers in request")
            } else if (headerCount == 1) {
                val header = request.headers["dpop"] ?: error("Dpop header not present")
                if (header.contains(',')) {
                    error("Multiple values in dpop header")
                }
                DPoPProof(SignedJWT.parse(request.headers["dpop"]))
            } else {
                null
            }
        }
    }

    init {
        if (jwt.header.type.type != DPOP_TYP) {
            error("Invalid dpop proof: invalid typ")
        }

        if (!JWSAlgorithm.Family.SIGNATURE.contains(jwt.header.algorithm)) {
            error("Invalid dpop proof: invalid alg")
        }

        if (jwt.header.jwk == null) {
            error("Invalid dpop proof: jwk header missing")
        }

        if (jwt.header.jwk.isPrivate) {
            error("Invalid dpop proof: jwk contains private key")
        }

        if (jwt.jwtClaimsSet.issueTime == null) {
            error("Invalid dpop proof: iat missing")
        }

        if (!jwt.verify(jwsVerifierForKey(jwt.header.jwk))) {
            error("Invalid dpop proof: invalid signature")
        }
    }

    val htm: String =
        jwt.jwtClaimsSet.getStringClaim("htm") ?: error("Invalid dpop proof: htm missing")

    val htu: String =
        jwt.jwtClaimsSet.getStringClaim("htu") ?: error("Invalid dpop proof: htu missing")

    val jti: String =
        jwt.jwtClaimsSet.getStringClaim("jti") ?: error("Invalid dpop proof: jti missing")

    val ath: String? = jwt.jwtClaimsSet.getStringClaim("ath")

    val nonce: String? = jwt.jwtClaimsSet.getStringClaim("nonce")

    val jwk: JWK = jwt.header.jwk
}
