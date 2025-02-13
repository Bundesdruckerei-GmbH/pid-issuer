/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp.dcql

import kotlinx.serialization.json.JsonElement

class JsonElementWithDistinctPath(
    val jsonElement: JsonElement,
    val path: DistinctClaimsPathPointer,
)
