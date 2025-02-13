/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.credentials

import de.bdr.openid4vc.common.vp.dcql.DistinctClaimsPathPointer

/**
 * A set of [DistinctClaimsPathPointer]s that defines a subset of disclosures in the credential that
 * shall be disclosed.
 */
typealias DisclosureSelection = Set<DistinctClaimsPathPointer>

/**
 * A [Credential] together with a [DisclosureSelection]. This combination can be used to construct a
 * concrete verifiable presentation including all claims that are always disclosed and the selected
 * claims from the selectively discloseable claims.
 */
data class CredentialWithDisclosureSelection(
    val credential: Credential,
    val toDisclose: DisclosureSelection,
)
