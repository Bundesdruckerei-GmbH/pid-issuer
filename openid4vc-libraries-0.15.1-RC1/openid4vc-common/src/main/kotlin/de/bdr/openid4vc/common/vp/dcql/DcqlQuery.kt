/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp.dcql

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DcqlQuery(
    @SerialName("credentials") val credentials: List<CredentialQuery>,
    @SerialName("credential_sets") val credentialSets: List<CredentialSetQuery>? = null,
) {
    init {
        require(credentials.isNotEmpty()) { "vp_query: Must have at least one credential" }
        val seenIds = mutableSetOf<String>()
        credentials
            .map { it.id }
            .forEach {
                require(seenIds.add(it)) { "vp_query: Duplicate credentials id ($it) in vp query" }
            }

        if (credentialSets != null) {
            require(credentialSets.isNotEmpty()) {
                "vp_query: Must have at least one credential set"
            }
            credentialSets.forEach { set -> set.validate(credentials.map { it.id }) }
        }
    }

    /*
     * Returns the credential_sets or a synthetic required credential set of all credential queries if they are not defined.
     */
    fun credentialSetsOrSyntheticCompleteSet(): List<CredentialSetQuery> {
        return credentialSets
            ?: listOf(CredentialSetQuery(options = listOf(credentials.map { it.id })))
    }
}
