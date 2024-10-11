/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common

import assertk.assertThat
import assertk.assertions.isNull
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UnsupportedCredentialDescriptionTest {

    @Test
    fun testDefaultValues() {
        val valueWithDefaultValues =
            UnsupportedCredentialDescription(format = UnsupportedCredentialFormat("unsupported"))

        assertThat(valueWithDefaultValues.cryptographicBindingMethodsSupported).isNull()
        assertThat(valueWithDefaultValues.cryptographicSigningAlgValuesSupported).isNull()
        assertThat(valueWithDefaultValues.display).isNull()
        assertThat(valueWithDefaultValues.proofTypesSupported).isNull()
        assertThat(valueWithDefaultValues.scope).isNull()
    }
}
