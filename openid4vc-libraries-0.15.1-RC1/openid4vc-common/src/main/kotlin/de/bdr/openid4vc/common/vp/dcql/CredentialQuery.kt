/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp.dcql

import de.bdr.openid4vc.common.CredentialFormat
import kotlinx.serialization.Serializable

@Serializable(with = CredentialQuerySerializer::class)
abstract class CredentialQuery {
    abstract val id: String
    abstract val format: CredentialFormat
    abstract val credentialMetaQuery: Any?
    abstract val claims: List<ClaimsQuery>?
    abstract val claimsSets: Set<Set<String>>?

    protected fun checkClaimsAndClaimSets(
        claims: List<ClaimsQuery>?,
        claimsSets: Set<Set<String>>?,
    ) {
        if (claims != null) {
            require(claims.isNotEmpty()) { "Claims must not be empty" }
            val claimQueryIds = claims.mapNotNull { it.id }
            require(claimQueryIds.size == claimQueryIds.distinct().size) {
                "Duplicate claim query id"
            }
        }

        if (claimsSets != null) {
            require(claims != null) { "Must have at least one claim query if claims_sets is used" }
            require(claims.all { it.id != null }) {
                "Id must be set for all claim queries if claims_sets is used"
            }
            require(claimsSets.isNotEmpty()) { "Must have at least one claim set" }
            claimsSets.forEach { set ->
                require(set.isNotEmpty()) { "Claim set must not be empty" }
                set.forEach { idFromSet ->
                    val idWithoutQuestionMark = idFromSet.removeSuffix("?")
                    require(idWithoutQuestionMark.isValidId()) {
                        "Invalid id $idFromSet in claim_sets"
                    }
                    require(claims.any { it.id == idWithoutQuestionMark }) {
                        "Id $idFromSet in claim_sets not found in claims"
                    }
                }
            }
        }
    }

    companion object {

        private val ID_REGEX = Regex("^[a-zA-Z0-9_-]+$")

        private fun String.isValidId() = ID_REGEX.matches(this)
    }
}
