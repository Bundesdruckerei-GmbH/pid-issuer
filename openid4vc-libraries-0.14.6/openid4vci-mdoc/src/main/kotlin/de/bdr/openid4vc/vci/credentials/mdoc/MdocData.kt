/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.vci.credentials.mdoc

import java.time.Instant

class MdocData(
    val validFrom: Instant? = null,
    val validUntil: Instant? = null,
    val namespaces: Map<String, Map<String, Any>>
)
