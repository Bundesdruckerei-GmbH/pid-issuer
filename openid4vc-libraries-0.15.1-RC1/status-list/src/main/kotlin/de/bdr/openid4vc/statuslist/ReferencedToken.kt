/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.statuslist

import com.nimbusds.jwt.JWT
import de.bdr.openid4vc.statuslist.StatusListException.Reason.INVALID_STATUS_REFERENCE
import java.net.URI
import java.net.URISyntaxException

/** Represents a referenced token as specified by draft-ietf-oauth-status-list 00. */
class ReferencedToken(val issuerUri: String, val statusListUri: String, val index: Int) {

    companion object {

        fun parse(serialized: String) = from(parseJwt(serialized))

        fun from(jwt: JWT): ReferencedToken {
            val issuerUri =
                jwt.jwtClaimsSet.issuer
                    ?: fail(
                        StatusListException.Reason.MISSING_CLAIMS,
                        "Missing iss claim in referenced token",
                    )
            val status =
                jwt.jwtClaimsSet.getClaim("status") as? Map<String, Any?>
                    ?: fail(
                        StatusListException.Reason.MISSING_CLAIMS,
                        "Missing status claim in referenced token",
                    )
            val statusList =
                status["status_list"] as? Map<String, Any?>
                    ?: fail(
                        StatusListException.Reason.MISSING_CLAIMS,
                        "Missing status_list claim in referenced token",
                    )

            if (statusList.size != 2 || statusList["idx"] !is Int || statusList["uri"] !is String)
                fail(
                    StatusListException.Reason.INVALID_CLAIM,
                    "Invalid status claim in referenced token",
                )
            val index = statusList.getValue("idx") as Int
            val statusListUri = statusList.getValue("uri") as String

            return ReferencedToken(issuerUri, statusListUri, index)
        }
    }

    fun verifiedStatus(provider: StatusListSource): Byte {
        val uri =
            try {
                URI(statusListUri)
            } catch (e: URISyntaxException) {
                fail(INVALID_STATUS_REFERENCE, cause = e)
            }
        return provider.get(uri).get(index)
    }
}
