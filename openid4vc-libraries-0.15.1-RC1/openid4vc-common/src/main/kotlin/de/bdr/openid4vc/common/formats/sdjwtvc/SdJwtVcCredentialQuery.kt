/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.formats.sdjwtvc

import de.bdr.openid4vc.common.CredentialFormat
import de.bdr.openid4vc.common.vp.dcql.ClaimsQuery
import de.bdr.openid4vc.common.vp.dcql.CredentialQuery
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SdJwtVcCredentialQuery(
    @SerialName("id") override val id: String,
    @SerialName("format") override val format: CredentialFormat,
    @SerialName("meta") override val credentialMetaQuery: SdJwtVcCredentialMetaQuery? = null,
    @SerialName("claims") override val claims: List<ClaimsQuery>? = null,
    @SerialName("claims_sets") override val claimsSets: Set<Set<String>>? = null,
) : CredentialQuery() {
    init {
        require(format == SdJwtVcCredentialFormat) { "Format must be sd jwt vc" }
        checkClaimsAndClaimSets(claims, claimsSets)
    }
}
