/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp.pex

import de.bdr.openid4vc.common.credentials.Credential
import de.bdr.openid4vc.common.credentials.DisclosureSelection
import de.bdr.openid4vc.common.vp.BaseCredentialQueryMatcher
import de.bdr.openid4vc.common.vp.SimpleJsonPathParser
import de.bdr.openid4vc.common.vp.dcql.ClaimsPathPointer
import de.bdr.openid4vc.common.vp.dcql.DistinctClaimsPathPointer
import de.bdr.openid4vc.common.vp.pex.LimitDisclosureSetting.REQUIRED
import jsonStringContent
import kotlin.reflect.KClass
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * An abstract base class that implements the generic matching process for an [InputDescriptor].
 *
 * Implementers may have a look at the existing implementations to understand how this can be
 * applied. The class is designed as is to allow reuse for JSON based and mdoc based credentials.
 */
abstract class BaseInputDescriptorMatcher<
    CredentialType : Credential,
    ClaimsDataType : Any,
    ClaimValueType : Any,
>(credentialClass: KClass<CredentialType>) :
    BaseCredentialQueryMatcher<InputDescriptor, CredentialType, ClaimsDataType, Field, Boolean>(
        credentialClass
    ) {

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
        credentialQuery: InputDescriptor,
        verificationSettings: Boolean,
        credential: CredentialType,
        disclosureSelection: DisclosureSelection,
    ) =
        if (
            verificationSettings && credentialQuery.constraints.limitDisclosureSetting == REQUIRED
        ) {
            disclosureSelection.size == credential.getDiscloseableClaims().size
        } else {
            true
        }

    override fun InputDescriptor.getClaimsSelection(): ClaimsSelection {
        return if (constraints.fields.isNullOrEmpty()) {
            NoClaims
        } else {
            ClaimsQuerySets(listOf(constraints.fields.toSet()))
        }
    }

    override fun Field.resolve(claims: ClaimsDataType): ClaimsQueryResolutionResult {
        val requiredClaims = mutableSetOf<DistinctClaimsPathPointer>()
        val forbiddenClaims = mutableSetOf<DistinctClaimsPathPointer>()
        path.firstOrNull { resolvePath(this, it, claims, requiredClaims, forbiddenClaims) }
        return ClaimsQueryResolutionResult(requiredClaims, forbiddenClaims)
    }

    private fun resolvePath(
        field: Field,
        path: String,
        claims: ClaimsDataType,
        requiredClaims: MutableSet<DistinctClaimsPathPointer>,
        forbiddenClaims: MutableSet<DistinctClaimsPathPointer>,
    ): Boolean {
        val resolved =
            SimpleJsonPathParser.parseSimpleJsonPathToClaimsPathPointer(path)
                .resolveClaimValuesWithPath(claims)
        if (resolved.isEmpty()) return false
        return if (field.filter == null) {
            resolved.mapTo(requiredClaims) { it.second }
            true
        } else {
            val filter = parseFilter(field.filter)

            resolved.forEach { (value, claim) ->
                if (value.toJsonPrimitive()?.let { filter.matches(it) } == true) {
                    requiredClaims.add(claim)
                } else {
                    forbiddenClaims.add(claim)
                }
            }

            return if (requiredClaims.isNotEmpty()) {
                true
            } else {
                requiredClaims.clear()
                forbiddenClaims.clear()
                false
            }
        }
    }

    companion object {
        private val SUPPORTED_FILTER_KEYS = setOf("const", "type")
    }

    private fun parseFilter(filter: JsonObject): Filter {
        val keys = filter.keys

        require(keys.union(SUPPORTED_FILTER_KEYS).size <= 2) { "Unsupported filter: $filter" }

        val constFilter =
            if (keys.contains("const")) {
                val const =
                    filter["const"] ?: throw IllegalArgumentException("Unsupported filter $filter")
                ConstFilter(const.jsonPrimitive)
            } else {
                null
            }

        val stringFilter =
            if (keys.contains("type")) {
                require(filter["type"]?.jsonStringContent() == "string") {
                    "Unsupported filter $filter"
                }
                StringTypeFilter
            } else {
                null
            }

        return when {
            constFilter != null && stringFilter != null ->
                CombinedFilter(listOf(constFilter, stringFilter))
            stringFilter != null -> stringFilter
            constFilter != null -> constFilter
            else -> AcceptAllFilter
        }
    }

    sealed interface Filter {
        fun matches(value: JsonPrimitive): Boolean
    }

    data object AcceptAllFilter : Filter {
        override fun matches(value: JsonPrimitive) = true
    }

    data class CombinedFilter(val filters: Collection<Filter>) : Filter {
        override fun matches(value: JsonPrimitive) = filters.all { it.matches(value) }
    }

    data class ConstFilter(val const: JsonPrimitive) : Filter {
        override fun matches(value: JsonPrimitive) = value == const
    }

    data object StringTypeFilter : Filter {
        override fun matches(value: JsonPrimitive) = value.isString
    }
}
