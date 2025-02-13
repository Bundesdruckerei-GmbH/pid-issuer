/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp.dcql

import de.bdr.openid4vc.common.credentials.Credential
import de.bdr.openid4vc.common.credentials.DisclosureSelection
import de.bdr.openid4vc.common.vp.BaseCredentialQueryMatcher
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlinx.serialization.json.JsonPrimitive

/**
 * An abstract base class that implements the generic matching process for a [CredentialQuery].
 *
 * Implementers may have a look at the existing implementations to understand how this can be
 * applied. The class is designed as is to allow reuse for JSON based and mdoc based credentials.
 */
abstract class BaseDcqlCredentialQueryMatcher<
    CredentialType : Credential,
    CredentialMetaQueryType : Any,
    ClaimsDataType : Any,
    ClaimValueType : Any,
>(
    credentialClass: KClass<CredentialType>,
    private val credentialMetaQueryClass: KClass<CredentialMetaQueryType>,
) :
    BaseCredentialQueryMatcher<
        CredentialQuery,
        CredentialType,
        ClaimsDataType,
        ClaimsQuery,
        VerificationSettings?,
    >(credentialClass) {

    /**
     * Checks if the meta query conditions are fulfilled by the given credential. This method is
     * invoked by `matchCredential` before any claims are matched. A return value of `false` aborts
     * the matching process and leads to a `null` return value from `matchCredential`.
     */
    abstract fun credentialFulfillsMetaQuery(
        credentialMetaQuery: CredentialMetaQueryType,
        credential: CredentialType,
    ): Boolean

    /**
     * Returns the path from the claims query as [ClaimsPathPointer]. This is an abstract to allow
     * use for JSON and mdoc based credentials.
     */
    abstract fun ClaimsQuery.getPath(): ClaimsPathPointer

    /**
     * Resolves a ClaimsPathPointer by applying it to the credentials claims data. The result is a
     * collection of matching claims as [Pair] of value and distinct path to the claim.
     */
    abstract fun ClaimsPathPointer.resolveClaimValuesWithPath(
        claims: ClaimsDataType
    ): Collection<Pair<ClaimValueType, DistinctClaimsPathPointer>>

    /**
     * Converts a claim value to a [JsonPrimitive] if this is possible.
     *
     * @return the resulting [JsonPrimitive] or `null`
     */
    abstract fun ClaimValueType.toJsonPrimitive(): JsonPrimitive?

    override fun verificationCheckSucceeds(
        credentialQuery: CredentialQuery,
        verificationSettings: VerificationSettings?,
        credential: CredentialType,
        disclosureSelection: DisclosureSelection,
    ) =
        if (
            verificationSettings != null && !verificationSettings.allowDisclosureOfUnnecessaryClaims
        ) {
            disclosureSelection.size == credential.getDiscloseableClaims().size
        } else {
            true
        }

    final override fun preCheckQuery(
        credentialQuery: CredentialQuery,
        credential: CredentialType,
    ): Boolean {
        val metaQuery = credentialQuery.credentialMetaQuery ?: return true
        if (!credentialMetaQueryClass.isInstance(metaQuery)) return false
        return credentialFulfillsMetaQuery(credentialMetaQueryClass.cast(metaQuery), credential)
    }

    override fun CredentialQuery.getClaimsSelection(): ClaimsSelection {
        val claims = claims
        val claimsSets = claimsSets
        return if (claims == null) {
            AllClaims
        } else if (claimsSets == null) {
            ClaimsQuerySets(listOf(claims.toSet()))
        } else {
            ClaimsQuerySets(
                claimsSets.map { claimsSet ->
                    claimsSet.mapTo(mutableSetOf()) { claimQueryId ->
                        claims.first { it.id == claimQueryId }
                    }
                }
            )
        }
    }

    override fun ClaimsQuery.resolve(claims: ClaimsDataType): ClaimsQueryResolutionResult {
        val resolved = getPath().resolveClaimValuesWithPath(claims)

        val requiredClaims = mutableSetOf<DistinctClaimsPathPointer>()
        val forbiddenClaims = mutableSetOf<DistinctClaimsPathPointer>()
        if (values == null) {
            resolved.mapTo(requiredClaims) { it.second }
        } else {
            resolved.forEach { (value, claim) ->
                if (values.contains(value.toJsonPrimitive())) {
                    requiredClaims.add(claim)
                } else {
                    forbiddenClaims.add(claim)
                }
            }
        }

        return ClaimsQueryResolutionResult(requiredClaims, forbiddenClaims)
    }
}
