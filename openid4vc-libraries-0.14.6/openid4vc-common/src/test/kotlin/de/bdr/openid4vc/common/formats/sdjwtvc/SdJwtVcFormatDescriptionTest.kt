/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.formats.sdjwtvc

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SdJwtVcFormatDescriptionTest {

    @Test
    fun testHashCodeAndEquals() {
        val a = SdJwtVcFormatDescription()
        val b = SdJwtVcFormatDescription()

        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }
}
