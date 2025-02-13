/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ClientIdSchemeTest {

    @Test
    fun `pre_registered_client_id`() {
        assertThat(ClientIdScheme.fromClientId("client_12335"))
            .isEqualTo(Pair(ClientIdScheme.PRE_REGISTERED, "client_12335"))
    }

    @Test
    fun `x509_san_dns client_id_scheme`() {
        assertThat(ClientIdScheme.fromClientId("x509_san_dns:client.example.org"))
            .isEqualTo(Pair(ClientIdScheme.X509_SAN_DNS, "client.example.org"))
    }

    @Test
    fun `x509_san_uri client_id_scheme`() {
        assertThat(ClientIdScheme.fromClientId("x509_san_uri:https://client.example.org/cb"))
            .isEqualTo(Pair(ClientIdScheme.X509_SAN_URI, "https://client.example.org/cb"))
    }

    @Test
    fun `redirect client_id_scheme`() {
        assertThat(ClientIdScheme.fromClientId("redirect_uri:https://client.example.org/2Fcb"))
            .isEqualTo(Pair(ClientIdScheme.REDIRECT_URI, "https://client.example.org/2Fcb"))
    }

    @Test
    fun `unknown client_id_scheme`() {
        assertThrows<IllegalArgumentException> {
            ClientIdScheme.fromClientId("unknown:https://client.example.org/2Fcb")
        }
    }
}
