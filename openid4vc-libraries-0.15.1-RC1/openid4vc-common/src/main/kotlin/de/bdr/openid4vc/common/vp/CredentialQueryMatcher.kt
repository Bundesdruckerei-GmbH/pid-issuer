/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp

import de.bdr.openid4vc.common.credentials.Credential
import de.bdr.openid4vc.common.credentials.DisclosureSelection

/**
 * An interface that performs the generic matching process for credentials to verifier requests.
 *
 * **Naming** The class uses the terms credential query and claims query. Even though those are
 * defined as part of DCQL, in the context of this class both are a more abstract concept.
 */
fun interface CredentialQueryMatcher<CredentialQueryType : Any, VerificationSettingsType : Any?> {

    /**
     * Attempts to find a combination of disclosures for the given credential that matches the
     * provided credential query. If multiple combinations match the query, the combination with the
     * highest priority (as defined in the query by the order of the claims_sets) is chosen.
     *
     * @param credentialQuery the credential query to match
     * @param credential the credential to match
     * @param verification specifies if the check is used during verification (not `null`) or in the
     *   wallet for credential selection (`null`, default). If used for verification, only
     *   combinations are considered a valid return value that include all disclosures. The method
     *   must return `null` otherwise. The verification settings then further specify the exact
     *   behaviour.
     * @return the matching combination with the highest priority or `null` if no combination can be
     *   found, the credential query does not have the same format as the credential or either the
     *   credential query type or credential type is not supported by this `CredentialQueryMatcher`
     */
    fun matchCredential(
        credentialQuery: CredentialQueryType,
        credential: Credential,
        verificationSettings: VerificationSettingsType,
    ): DisclosureSelection?
}
