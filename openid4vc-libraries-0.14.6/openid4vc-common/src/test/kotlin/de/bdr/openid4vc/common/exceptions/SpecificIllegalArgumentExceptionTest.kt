/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.exceptions

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException.ReasonCode.INVALID_PROOF
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SpecificIllegalArgumentExceptionTest {
    @Test
    fun `given a message,cause and reason when constructed then all parameters are set correctly`() {
        val message = "Message"
        val cause = Throwable()
        val exception = SpecificIllegalArgumentException(INVALID_PROOF, message, cause)

        assertThat(exception.reason).isEqualTo(INVALID_PROOF)
        assertThat(exception.message).isEqualTo(message)
        assertThat(exception.cause).isEqualTo(cause)
    }

    @Test
    fun `given a message, reason and now cause when constructed then all parameters are set correctly`() {
        val message = "Message"
        val exception = SpecificIllegalArgumentException(INVALID_PROOF, message)

        assertThat(exception.reason).isEqualTo(INVALID_PROOF)
        assertThat(exception.message).isEqualTo(message)
        assertThat(exception.cause).isNull()
    }
}
