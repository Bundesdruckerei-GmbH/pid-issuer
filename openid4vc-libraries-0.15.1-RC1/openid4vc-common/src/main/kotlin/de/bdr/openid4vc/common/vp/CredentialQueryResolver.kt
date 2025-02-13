/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp

import de.bdr.openid4vc.common.CredentialFormat
import de.bdr.openid4vc.common.credentials.Credential
import de.bdr.openid4vc.common.credentials.CredentialWithDisclosureSelection
import de.bdr.openid4vc.common.credentials.DisclosureSelection
import de.bdr.openid4vc.common.vp.dcql.CredentialQuery
import de.bdr.openid4vc.common.vp.dcql.DcqlQuery
import de.bdr.openid4vc.common.vp.dcql.VerificationSettings
import de.bdr.openid4vc.common.vp.pex.InputDescriptor
import de.bdr.openid4vc.common.vp.pex.PresentationDefinition

private typealias CredentialQueryId = String

/**
 * Implements credential query resolution against a set of input credentials.
 *
 * Supports both DCQL queries and presentation definitions.
 *
 * This implementation performs the matching of credential sets but delegates the credential
 * matching process to the [CredentialQueryMatcher]s registered through the [CredentialFormat].
 */
object CredentialQueryResolver {

    /**
     * Resolves all credential combinations of the given credentials that fulfill the provided DCQL
     * query.
     *
     * As demanded by DCQL all combinations of credentials to fulfill the credential sets are
     * provided but only one claims configuration per credential query is chosen based on the
     * priority in the query.
     *
     * @param credentials the credentials to check
     * @param query the DCQL query to use
     * @return the credential combinations that fulfill the query as a `List`. Each element is a Map
     *   with a credential and disclosure selection as value and the id of the matching credential
     *   query as key.
     */
    fun resolve(
        credentials: Collection<Credential>,
        query: DcqlQuery,
    ): List<Map<String, CredentialWithDisclosureSelection>> =
        resolve(
            credentials,
            query,
            null,
            getCombinedCredentialOptions = ::combinedCredentialOptions,
            getCredentialQueries = DcqlQuery::credentials,
            getCredentialQueryId = CredentialQuery::id,
            matchCredential = ::matchCredential,
        )

    /**
     * Verifies that the given credentials fulfill the provided DCQL query.
     *
     * @param credentials the credentials to check, the keys are the credentialQueryIds from the
     *   credential response
     * @param query the DCQL query to use
     * @return the credentials with the associated disclosure information demanded by the query.
     *   This serves to detect additional disclosures, that were not requested. On verification
     *   failure, `null` is returned.
     */
    fun verify(
        credentials: Map<String, Credential>,
        query: DcqlQuery,
        verificationSettings: VerificationSettings,
    ) =
        resolve(
                credentials.values.toSet(),
                query,
                verificationSettings,
                getCombinedCredentialOptions = ::combinedCredentialOptions,
                getCredentialQueries = DcqlQuery::credentials,
                getCredentialQueryId = CredentialQuery::id,
                matchCredential = ::matchCredential,
                verifiedCredentialAssociation = credentials,
            )
            .firstOrNull()

    /**
     * Resolves all credential combinations of the given credentials that fulfill the provided
     * presentation definition query.
     *
     * All combinations of credentials to fulfill the credential sets are provided.
     *
     * @param credentials the credentials to check
     * @param presentationDefinition the presentation definition to use
     * @return the credential combinations that fulfill the query as a `List`. Each element is a Map
     *   with a credential and disclosure selection as value and the id of the matching input
     *   descriptor as key.
     */
    fun resolve(
        credentials: Collection<Credential>,
        presentationDefinition: PresentationDefinition,
    ): List<Map<String, CredentialWithDisclosureSelection>> =
        resolve(
            credentials,
            presentationDefinition,
            false,
            getCombinedCredentialOptions = ::combinedCredentialOptions,
            getCredentialQueries = PresentationDefinition::inputDescriptors,
            getCredentialQueryId = InputDescriptor::id,
            matchCredential = ::matchCredential,
        )

    /**
     * Verifies that the given credentials fulfill the provided presentation definition query.
     *
     * @param credentials the credentials to check, the keys are the input descriptor ids
     * @param presentationDefinition the presentation definition to use
     * @return the credentials with the associated disclosure information demanded by the query.
     *   This serves to detect additional disclosures, that were not requested. On verification
     *   failure, `null` is returned.
     */
    fun verify(
        credentials: Map<String, Credential>,
        presentationDefinition: PresentationDefinition,
    ) =
        resolve(
                credentials.values.toSet(),
                presentationDefinition,
                true,
                getCombinedCredentialOptions = ::combinedCredentialOptions,
                getCredentialQueries = PresentationDefinition::inputDescriptors,
                getCredentialQueryId = InputDescriptor::id,
                matchCredential = ::matchCredential,
                verifiedCredentialAssociation = credentials,
            )
            .firstOrNull()

    /**
     * @param credentials the credentials to consider
     * @param query the query
     * @param verificationSettings used during verification to transport verification settings
     * @param getCombinedCredentialOptions returns sets of credential query id combinations allowed
     *   by the query, if verifiedCredentialAssociation is set, this value is not used but the
     *   combination from there is considered
     * @param getCredentialQueries the credential queries in the query
     * @param matchCredential attempts to matche a credential with a credential query
     * @param verifiedCredentialAssociation used during verification to restrict the considered
     *   results to a specific association of credential queries to credentials, the method will
     *   exit early as soon as a violation of this is detected
     */
    private fun <VerificationSettingsType : Any?, QueryType, CredentialQueryType> resolve(
        credentials: Collection<Credential>,
        query: QueryType,
        verificationSettings: VerificationSettingsType,
        getCombinedCredentialOptions: QueryType.() -> Set<Set<String>>,
        getCredentialQueries: QueryType.() -> Iterable<CredentialQueryType>,
        getCredentialQueryId: CredentialQueryType.() -> String,
        matchCredential:
            (CredentialQueryType, Credential, VerificationSettingsType) -> DisclosureSelection?,
        verifiedCredentialAssociation: Map<String, Credential>? = null,
    ): List<Map<String, CredentialWithDisclosureSelection>> {
        var combinedCredentialOptions = query.getCombinedCredentialOptions()

        if (verifiedCredentialAssociation != null) {
            if (!combinedCredentialOptions.contains(verifiedCredentialAssociation.keys)) {
                return emptyList()
            }
            combinedCredentialOptions = setOf(verifiedCredentialAssociation.keys)
        }

        val credentialQueryIdsAndMatchingCredentials =
            query.getCredentialQueries().associate { credentialQuery ->
                Pair(
                    credentialQuery.getCredentialQueryId(),
                    credentials.mapNotNull { credential ->
                        if (
                            verifiedCredentialAssociation == null ||
                                verifiedCredentialAssociation[
                                    credentialQuery.getCredentialQueryId()] == credential
                        ) {
                            matchCredential(credentialQuery, credential, verificationSettings)
                                ?.let { CredentialWithDisclosureSelection(credential, it) }
                        } else {
                            null
                        }
                    },
                )
            }

        val optionsWithCredentialQueryIdsAndMatchingCredentials =
            combinedCredentialOptions.mapNotNull { option ->
                option.map { credentialQueryId ->
                    val credentialsForQueryId =
                        credentialQueryIdsAndMatchingCredentials[credentialQueryId]
                    if (credentialsForQueryId?.isNotEmpty() == true) {
                        Pair(credentialQueryId, credentialsForQueryId)
                    } else {
                        return@mapNotNull null
                    }
                }
            }

        return optionsWithCredentialQueryIdsAndMatchingCredentials.flatMap {
            optionWithCredentialQueryIdsAndMatchingCredentials ->
            allCredentialCombinationsPerOption(optionWithCredentialQueryIdsAndMatchingCredentials)
        }
    }

    private fun combinedCredentialOptions(
        presentationDefinition: PresentationDefinition
    ): Set<Set<String>> {
        // TODO: This is the place where submission requirements would be implemented and handled.
        // For now the implementation is trivial because only one combination, the one matching all
        // input descriptors, is valid.
        return setOf(presentationDefinition.inputDescriptors.mapTo(mutableSetOf()) { it.id })
    }

    private fun combinedCredentialOptions(dcqlQuery: DcqlQuery): Set<Set<String>> {
        val optionsFromAllCredentialSets =
            dcqlQuery.credentialSetsOrSyntheticCompleteSet().map { credentialSetQuery ->
                if (credentialSetQuery.required) {
                    credentialSetQuery.options
                } else {
                    credentialSetQuery.options.union(listOf(emptyList()))
                }
            }
        val result = mutableSetOf<Set<String>>()
        combineCredentialOptions(optionsFromAllCredentialSets, null, result::add)
        return result
    }

    private fun matchCredential(
        credentialQuery: CredentialQuery,
        credential: Credential,
        verificationSettings: VerificationSettings?,
    ): DisclosureSelection? {
        if (credential.format != credentialQuery.format) return null
        return credential.format.credentialQueryMatcher.matchCredential(
            credentialQuery,
            credential,
            verificationSettings,
        )
    }

    private fun matchCredential(
        inputDescriptor: InputDescriptor,
        credential: Credential,
        verification: Boolean,
    ): DisclosureSelection? {
        if (credential.format != inputDescriptor.format.type) return null
        return credential.format.inputDescriptorMatcher.matchCredential(
            inputDescriptor,
            credential,
            verification,
        )
    }

    private fun allCredentialCombinationsPerOption(
        optionWithCredentialQueryIdsAndMatchingCredentials:
            List<Pair<CredentialQueryId, List<CredentialWithDisclosureSelection>>>
    ): Iterable<Map<CredentialQueryId, CredentialWithDisclosureSelection>> {
        val result = mutableListOf<Map<CredentialQueryId, CredentialWithDisclosureSelection>>()
        computeCredentialCombinationsPerOption(
            optionWithCredentialQueryIdsAndMatchingCredentials,
            null,
            result::add,
        )
        return result
    }

    private fun combineCredentialOptions(
        optionsFromAllCredentialSets: List<Collection<List<String>>>,
        element: OptionsToCombine?,
        onOptionsCombined: (Set<String>) -> Unit,
    ) {
        if (optionsFromAllCredentialSets.isEmpty()) {
            element?.let { onOptionsCombined(it.toSet()) }
        } else {
            optionsFromAllCredentialSets.first().forEach {
                combineCredentialOptions(
                    optionsFromAllCredentialSets.drop(1),
                    element.and(it),
                    onOptionsCombined,
                )
            }
        }
    }

    private fun computeCredentialCombinationsPerOption(
        optionWithCredentialQueryIdsAndMatchingCredentials:
            List<Pair<CredentialQueryId, List<CredentialWithDisclosureSelection>>>,
        element: CredentialQueryIdAndCredentialToCombine?,
        onCredentialsCombined: (Map<CredentialQueryId, CredentialWithDisclosureSelection>) -> Unit,
    ) {
        if (optionWithCredentialQueryIdsAndMatchingCredentials.isEmpty()) {
            element?.let { onCredentialsCombined(it.toMap()) }
        } else {
            val withoutFirst = optionWithCredentialQueryIdsAndMatchingCredentials.drop(1)
            val (credentialQueryId, credentials) =
                optionWithCredentialQueryIdsAndMatchingCredentials.first()
            credentials.forEach { credential ->
                computeCredentialCombinationsPerOption(
                    withoutFirst,
                    element.and(credentialQueryId, credential),
                    onCredentialsCombined,
                )
            }
        }
    }
}

private fun CredentialQueryIdAndCredentialToCombine?.and(
    credentialQueryId: CredentialQueryId,
    credential: CredentialWithDisclosureSelection,
) = CredentialQueryIdAndCredentialToCombine(credentialQueryId, credential, this)

private class CredentialQueryIdAndCredentialToCombine(
    val credentialQueryId: CredentialQueryId,
    val credential: CredentialWithDisclosureSelection,
    private val next: CredentialQueryIdAndCredentialToCombine?,
) {
    fun toMap(): Map<CredentialQueryId, CredentialWithDisclosureSelection> {
        val map = mutableMapOf<CredentialQueryId, CredentialWithDisclosureSelection>()
        var current: CredentialQueryIdAndCredentialToCombine? = this
        while (current != null) {
            map[current.credentialQueryId] = current.credential
            current = current.next
        }
        return map
    }
}

private fun OptionsToCombine?.and(credentialOption: List<String>) =
    OptionsToCombine(credentialOption, this)

private class OptionsToCombine(
    private val credentialOption: List<String>,
    private val next: OptionsToCombine?,
) {

    fun toSet(): Set<String> {
        val set = mutableSetOf<String>()
        var current: OptionsToCombine? = this
        while (current != null) {
            set.addAll(current.credentialOption)
            current = current.next
        }
        return set
    }
}
