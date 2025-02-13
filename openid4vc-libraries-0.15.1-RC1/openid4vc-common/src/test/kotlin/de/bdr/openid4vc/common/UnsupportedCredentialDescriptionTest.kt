/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
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
