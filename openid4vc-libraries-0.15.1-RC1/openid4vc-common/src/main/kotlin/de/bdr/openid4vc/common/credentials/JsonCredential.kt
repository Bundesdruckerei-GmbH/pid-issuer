/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.credentials

import de.bdr.openid4vc.common.vp.dcql.DistinctClaimsPathPointer
import kotlinx.serialization.json.JsonObject

/** A credential based on a JSON data structure. */
interface JsonCredential : Credential {

    /** @return the claims in this credential as a JsonObject. */
    val claims: JsonObject

    fun claims(toDisclose: Set<DistinctClaimsPathPointer>): JsonObject

    /** @return the claims in this credential as a JsonObject. */
    val discloseable: Set<DistinctClaimsPathPointer>
}
