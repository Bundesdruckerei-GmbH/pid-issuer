/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
