/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.base.requests

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MsoMdocAuthChannelCredentialFormatTest {
    @DisplayName("Should register the credential format")
    @Test
    fun test001() {
        // Given
        val subject = MsoMdocAuthChannelCredentialFormat

        // When

        // Then
        MatcherAssert.assertThat(subject.registered, Matchers.`is`(true))
    }
}