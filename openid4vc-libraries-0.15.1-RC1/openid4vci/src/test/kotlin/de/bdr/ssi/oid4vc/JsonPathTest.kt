/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.ssi.oid4vc

import de.bdr.openid4vc.vci.utils.removeRootPathPrefix
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JsonPathTest {

    @Test
    fun `test prefix remove from JsonPath`() {
        assertEquals("root.example", removeRootPathPrefix("$.root.example"))
        assertEquals("root.example", removeRootPathPrefix("root.example"))
        assertEquals("['root']['example']", removeRootPathPrefix("$['root']['example']"))
    }
}
