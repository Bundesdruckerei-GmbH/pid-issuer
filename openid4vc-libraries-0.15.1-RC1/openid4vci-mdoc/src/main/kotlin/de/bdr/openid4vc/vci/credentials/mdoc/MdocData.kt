/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.vci.credentials.mdoc

import java.time.Instant

class MdocData(
    val validFrom: Instant? = null,
    val validUntil: Instant? = null,
    val namespaces: Map<String, Map<String, Any>>
)
