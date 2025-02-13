/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist

import java.nio.charset.StandardCharsets.UTF_8

object ProjectVersion {
    val value =
        ProjectVersion::class
            .java
            .getResourceAsStream("/version.txt")
            ?.readAllBytes()
            ?.toString(UTF_8) ?: "-"
}
