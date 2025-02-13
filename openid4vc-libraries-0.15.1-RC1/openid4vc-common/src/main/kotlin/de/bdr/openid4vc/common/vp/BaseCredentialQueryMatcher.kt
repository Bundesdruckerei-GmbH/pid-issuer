/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp

import de.bdr.openid4vc.common.credentials.Credential
import de.bdr.openid4vc.common.credentials.DisclosureSelection
import de.bdr.openid4vc.common.toSetWithout
import de.bdr.openid4vc.common.vp.dcql.DistinctClaimsPathPointer
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * An abstract base class that implements the generic matching process for credentials to verifier
 * requests.
 *
 * **Naming** The class uses the terms credential query and claims query. Even though those are
 * defined as part of DCQL, in the context of this class both are a more abstract concept. A
 * credential query is a ruleset that can be matched against a credential. A credential can either
 * match or not match the query. If it matches this comes together with a specific configuration of
 * disclosed elements represented by a [DisclosureSelection]. A credential query can consist of
 * multiple claim query sets. Each claim query set is a combination of claim queries that, if
 * applied all at the same time, match or do not match a credential. A credential query matches if
 * one of the claim sets matches. Each claim query itself can be resolved against the claims of a
 * credential. Resolution yields a list of claims that are to be disclosed by the claim query (the
 * rule is, that each claim query must match at least one disclosed claim) and a list of claims that
 * are forbidden to be disclosed. Eventual conflicts between those rules are detected by this
 * implementation and will lead to a non-matching claims query set.
 *
 * The general matching process implemented here is valid for DCQL and presentation exchange
 * queries. Implementers may have a look at the existing subclasses to understand how this can be
 * applied.
 */
abstract class BaseCredentialQueryMatcher<
    CredentialQueryType : Any,
    CredentialType : Credential,
    ClaimsDataType : Any,
    ClaimsQueryType : Any,
    VerificationSettingsType : Any?,
>(private val credentialClass: KClass<CredentialType>) :
    CredentialQueryMatcher<CredentialQueryType, VerificationSettingsType> {

    /**
     * Performs a pre-check of the credential against the query. If it fails, no further matching
     * checks are performed and matching immediately fails.
     *
     * The default implementation returns `true`.
     *
     * @return `true` if the pre-check succeeds and matching process can continue, `false` to fail
     *   the matching process
     */
    open fun preCheckQuery(
        credentialQuery: CredentialQueryType,
        credential: CredentialType,
    ): Boolean {
        return true
    }

    /** @return the [ClaimsSelection] performed by the credential query. */
    abstract fun CredentialQueryType.getClaimsSelection(): ClaimsSelection

    /**
     * This is only invoked once and serves as a caching mean. Implementers that do not need this
     * may simply use the CredentialType as ClaimsDataType.
     *
     * @return the claims data from the credential.
     */
    abstract fun CredentialType.getClaims(): ClaimsDataType

    /** @return the set of claims that are selectively discloseable. */
    abstract fun CredentialType.getDiscloseableClaims(): Set<DistinctClaimsPathPointer>

    /**
     * Resolves a claims query against the credential claims.
     *
     * @return a [ClaimsQueryResolutionResult] that contains the required and forbidden claims
     */
    abstract fun ClaimsQueryType.resolve(claims: ClaimsDataType): ClaimsQueryResolutionResult

    /**
     * Performs a post-check of the disclosure selection in case of a verification check. This
     * allows to further restrict the matching credentials if a verification is performed. This
     * allows to use the same code for wallet and verifier. In case of a verifier this could for
     * example require, that no unnecessary disclosures are contained in the credential.
     */
    open fun verificationCheckSucceeds(
        credentialQuery: CredentialQueryType,
        verificationSettings: VerificationSettingsType,
        credential: CredentialType,
        disclosureSelection: DisclosureSelection,
    ): Boolean {
        return true
    }

    final override fun matchCredential(
        credentialQuery: CredentialQueryType,
        credential: Credential,
        verificationSettings: VerificationSettingsType,
    ): DisclosureSelection? {
        if (!credentialClass.isInstance(credential)) return null

        if (!preCheckQuery(credentialQuery, credentialClass.cast(credential))) {
            return null
        }

        return internalMatchCredential(
            credentialQuery,
            credentialClass.cast(credential),
            verificationSettings,
        )
    }

    private fun internalMatchCredential(
        credentialQuery: CredentialQueryType,
        credential: CredentialType,
        verificationSettings: VerificationSettingsType,
    ): DisclosureSelection? {
        return when (val claimsSelection = credentialQuery.getClaimsSelection()) {
            is AllClaims -> fullDisclosureSelection(credential)
            is NoClaims -> emptySet()
            is BaseCredentialQueryMatcher<*, *, *, *, *>.ClaimsQuerySets -> {
                val claimsQueryMatcher = ClaimsQueryMatcher(credential)
                val disclosureSelection =
                    claimsSelection.rules.firstNotNullOfOrNull {
                        claimsQueryMatcher.matchClaimsQueries(it as Collection<ClaimsQueryType>)
                    }
                return if (
                    verificationSettings == null ||
                        disclosureSelection != null &&
                            verificationCheckSucceeds(
                                credentialQuery,
                                verificationSettings,
                                credential,
                                disclosureSelection,
                            )
                ) {
                    disclosureSelection
                } else {
                    null
                }
            }
        }
    }

    private fun fullDisclosureSelection(credential: CredentialType) =
        credential.getDiscloseableClaims()

    /**
     * Instantiated during the matching process to perform matching of individual claims sets and
     * claim queries.
     *
     * This class is stateful and stores results from previous invocations. This allows for a more
     * efficient implementation because the claim queries are only resolved once.
     */
    private inner class ClaimsQueryMatcher(credential: CredentialType) {

        private val claims = credential.getClaims()

        private val discloseableWithChildPathsBeforeParentPaths = run {
            val discloseable = credential.getDiscloseableClaims()
            val result = ArrayList<DistinctClaimsPathPointer>(discloseable.size + 1)
            result.addAll(discloseable)
            result.add(DistinctClaimsPathPointer.ROOT)
            result.sortByDescending { it.toJsonPointer() }
            result
        }

        private val claimsQueryMatchCache = mutableMapOf<ClaimsQueryType, ClaimsQueryMatch?>()

        /**
         * Tries to find a [DisclosureSelection] that fulfills the combination of the given claim
         * queries.
         */
        fun matchClaimsQueries(claimsQueries: Collection<ClaimsQueryType>): DisclosureSelection? {
            val matches = mutableListOf<ClaimsQueryMatch>()
            claimsQueries.forEach { claimsQuery ->
                val match = claimsQuery.match() ?: return null

                matches.forEach { previousMatch ->
                    previousMatch.matchingDisclosures.removeForbiddenDisclosures(
                        match.forbiddenDisclosures
                    )
                    if (previousMatch.matchingDisclosures.isEmpty()) return null
                }

                matches.add(match)
                matches.forEach { aMatch ->
                    match.matchingDisclosures.removeForbiddenDisclosures(
                        aMatch.forbiddenDisclosures
                    )
                    if (match.matchingDisclosures.isEmpty()) return null
                }
            }
            return matches
                .asSequence()
                .flatMap { it.matchingDisclosures }
                .flatMap { it.andParentDisclosures() }
                .toSetWithout(DistinctClaimsPathPointer.ROOT)
        }

        private fun DistinctClaimsPathPointer.andParentDisclosures():
            Sequence<DistinctClaimsPathPointer> {
            return discloseableWithChildPathsBeforeParentPaths.asSequence().filter {
                this.startsWith(it)
            }
        }

        private fun ClaimsQueryType.match(): ClaimsQueryMatch? =
            claimsQueryMatchCache.computeIfAbsent(this) {
                ClaimsQueryMatch(this, claims, discloseableWithChildPathsBeforeParentPaths)
            }
    }

    /** Information about a matching claim query. */
    private inner class ClaimsQueryMatch(
        /** Disclosures that are required when applying the claim query match. */
        val matchingDisclosures: MutableSet<DistinctClaimsPathPointer>,
        /** Disclosures that are forbidden when applying the claim query match. */
        val forbiddenDisclosures: DisclosureSelection,
    )

    /**
     * Attempts to match the given [ClaimsQuery] with the credential
     *
     * @param query the claims query
     * @param claims the claims data of the credential
     * @param discloseable the selectively discloseable paths with child paths ordered before parent
     *   paths and including the [DistinctClaimsPathPointer.ROOT] path
     * @return a `ClaimQueryMatch` on success or `null` if it does not match
     */
    private fun ClaimsQueryMatch(
        query: ClaimsQueryType,
        claims: ClaimsDataType,
        discloseable: List<DistinctClaimsPathPointer>,
    ): ClaimsQueryMatch? {
        val resolved = query.resolve(claims)

        if (resolved.matchingClaims.isEmpty()) return null

        val matchingDisclosures =
            resolved.matchingClaims
                .map { matchingPath ->
                    discloseable.findMostSpecificDiscloseableThatContains(matchingPath)
                }
                .toMutableSet()

        val forbiddenDisclosures =
            resolved.forbiddenClaims
                .map { forbiddenPath ->
                    discloseable.findMostSpecificDiscloseableThatContains(forbiddenPath)
                }
                .toSet()

        return ClaimsQueryMatch(matchingDisclosures, forbiddenDisclosures)
    }

    private fun MutableCollection<DistinctClaimsPathPointer>.removeForbiddenDisclosures(
        forbidden: Collection<DistinctClaimsPathPointer>
    ) = removeIf { path -> forbidden.any { forbidden -> path.startsWith(forbidden) } }

    private fun List<DistinctClaimsPathPointer>.findMostSpecificDiscloseableThatContains(
        path: DistinctClaimsPathPointer
    ) = first { discloseable -> path.startsWith(discloseable) }

    /** Information about a claim query. */
    inner class ClaimsQueryResolutionResult(
        /** Claims that are matched by the claim query. */
        val matchingClaims: DisclosureSelection,
        /** Claims that are forbidden by the claim query. */
        val forbiddenClaims: DisclosureSelection,
    )

    /** A selection of claims denoted by the credential query. */
    sealed interface ClaimsSelection

    /** Selects all claims from the credential. */
    data object AllClaims : ClaimsSelection

    /**
     * Selects no claims from the credential, effectively omitting all selectively discloseable
     * claims.
     */
    data object NoClaims : ClaimsSelection

    /** Selects on of the given claim query sets from the credential. */
    inner class ClaimsQuerySets(val rules: Collection<Set<ClaimsQueryType>>) : ClaimsSelection
}
