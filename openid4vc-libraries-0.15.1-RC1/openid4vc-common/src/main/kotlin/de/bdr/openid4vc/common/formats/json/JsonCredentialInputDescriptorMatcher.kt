/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.formats.json

import de.bdr.openid4vc.common.credentials.JsonCredential
import de.bdr.openid4vc.common.vp.dcql.ClaimsPathPointer
import de.bdr.openid4vc.common.vp.dcql.DistinctClaimsPathPointer
import de.bdr.openid4vc.common.vp.pex.BaseInputDescriptorMatcher
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/** Matches JSON based credentials against input descriptors. */
object JsonCredentialInputDescriptorMatcher :
    BaseInputDescriptorMatcher<JsonCredential, JsonElement, JsonElement>(JsonCredential::class) {

    override fun JsonCredential.getClaims() = claims

    override fun JsonCredential.getDiscloseableClaims() = discloseable

    override fun ClaimsPathPointer.resolveClaimValuesWithPath(
        claims: JsonElement
    ): Collection<Pair<JsonElement, DistinctClaimsPathPointer>> {
        return resolveWithDistinctPaths(claims).mapTo(mutableSetOf()) {
            Pair(it.jsonElement, it.path)
        }
    }

    override fun JsonElement.toJsonPrimitive() = this as? JsonPrimitive
}
