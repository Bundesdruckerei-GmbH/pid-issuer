/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.formats.msomdoc

import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBORType
import de.bdr.openid4vc.common.vp.dcql.ClaimsPathPointer
import de.bdr.openid4vc.common.vp.dcql.DistinctClaimsPathPointer
import de.bdr.openid4vc.common.vp.dcql.ObjectElementSelector
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

internal object MatcherUtils {
    fun CBORObject.toJsonPrimitive() =
        if (isNull) {
            JsonNull
        } else {
            when (type) {
                CBORType.Boolean -> JsonPrimitive(AsBoolean())
                CBORType.FloatingPoint -> JsonPrimitive(AsDouble())
                CBORType.Integer -> JsonPrimitive(AsInt64Value())
                CBORType.TextString -> JsonPrimitive(AsString())
                else -> null
            }
        }

    fun MsoMdocCredential.getDiscloseableClaims() =
        namespacesAndValues.flatMapTo(mutableSetOf()) { (namespace, claims) ->
            claims.keys.map { claimName -> DistinctClaimsPathPointer(namespace, claimName) }
        }

    fun ClaimsPathPointer.resolveClaimValuesWithPath(
        claims: Map<String, Map<String, CBORObject>>
    ): Collection<Pair<CBORObject, DistinctClaimsPathPointer>> =
        if (
            this !is DistinctClaimsPathPointer ||
                selectors.size != 2 ||
                selectors.any { it !is ObjectElementSelector }
        ) {
            emptySet()
        } else {
            val namespace = (selectors[0] as ObjectElementSelector).claimName
            val claimName = (selectors[1] as ObjectElementSelector).claimName
            val value = claims[namespace]?.get(claimName)
            if (value == null) {
                emptySet()
            } else {
                setOf(Pair(value, DistinctClaimsPathPointer(namespace, claimName)))
            }
        }
}

internal fun DistinctClaimsPathPointer(namespace: String, claimName: String) =
    DistinctClaimsPathPointer(
        listOf(ObjectElementSelector(namespace), ObjectElementSelector(claimName))
    )
