/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.formats.json

import de.bdr.openid4vc.common.credentials.JsonCredential
import de.bdr.openid4vc.common.vp.dcql.BaseDcqlCredentialQueryMatcher
import de.bdr.openid4vc.common.vp.dcql.ClaimsPathPointer
import de.bdr.openid4vc.common.vp.dcql.ClaimsQuery
import de.bdr.openid4vc.common.vp.dcql.DistinctClaimsPathPointer
import kotlin.reflect.KClass
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * An abstract base class to match JSON based credentials against DCQL credential queries.
 *
 * Implementers will have to invoke the constructor with the concrete subclasses of [JsonCredential]
 * and the credential meta query type to use and implement the `checkMetaQuery` method. All other
 * matching logic based on the JSON claims and paths in the query will be performed by this base
 * class.
 */
abstract class JsonCredentialDcqlCredentialQueryMatcher<
    CredentialType : JsonCredential,
    CredentialMetaQueryType : Any,
>(
    credentialClass: KClass<CredentialType>,
    credentialMetaQueryClass: KClass<CredentialMetaQueryType>,
) :
    BaseDcqlCredentialQueryMatcher<
        CredentialType,
        CredentialMetaQueryType,
        JsonElement,
        JsonElement,
    >(credentialClass, credentialMetaQueryClass) {

    override fun ClaimsQuery.getPath(): ClaimsPathPointer =
        path ?: error("Missing path in claims query")

    override fun CredentialType.getClaims() = claims

    override fun CredentialType.getDiscloseableClaims() = discloseable

    override fun ClaimsPathPointer.resolveClaimValuesWithPath(
        claims: JsonElement
    ): Collection<Pair<JsonElement, DistinctClaimsPathPointer>> {
        return resolveWithDistinctPaths(claims).mapTo(mutableSetOf()) {
            Pair(it.jsonElement, it.path)
        }
    }

    override fun JsonElement.toJsonPrimitive() = this as? JsonPrimitive
}
