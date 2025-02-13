/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AuthorizationRequestByReferenceTest {
    @Test
    fun `deserialize example url with post`() {
        val exampleUri =
            "/authorize?client_id=x509_san_dns:client.example.org&client_metadata=%7B%7D&request_uri=https%3A%2F%2Fclient.example.org%2Frequest%2Fvapof4ql2i7m41m68uep&request_uri_method=post"

        val request =
            AuthorizationRequestBaseClass.fromUriString(exampleUri)
                as AuthorizationRequestByReference

        assertThat(request.clientId).isEqualTo("x509_san_dns:client.example.org")
        assertThat(request.clientMetadata).isEqualTo(JsonObject(emptyMap()))
        assertThat(request.requestUriMethod).isEqualTo(RequestUriMethod.POST)
        assertThat(request.requestUri)
            .isEqualTo("https://client.example.org/request/vapof4ql2i7m41m68uep")
    }

    @Test
    fun `deserialize example url with get`() {
        val exampleUri =
            "/authorize?client_id=x509_san_dns:client.example.org&request_uri=https%3A%2F%2Fclient.example.org%2Frequest%2Fvapof4ql2i7m41m68uep&request_uri_method=get"

        val request =
            AuthorizationRequestBaseClass.fromUriString(exampleUri)
                as AuthorizationRequestByReference

        assertThat(request.clientId).isEqualTo("x509_san_dns:client.example.org")
        assertThat(request.clientMetadata).isNull()
        assertThat(request.requestUriMethod).isEqualTo(RequestUriMethod.GET)
        assertThat(request.requestUri)
            .isEqualTo("https://client.example.org/request/vapof4ql2i7m41m68uep")
    }

    @Test
    fun `deserialize example url with unknown parameters`() {
        val exampleUri =
            "/authorize?client_id=x509_san_dns:client.example.org&client_metadata=%7B%7D&request_uri=https%3A%2F%2Fclient.example.org%2Frequest%2Fvapof4ql2i7m41m68uep&request_uri_method=post&param1=val1&param2"

        val request =
            AuthorizationRequestBaseClass.fromUriString(exampleUri)
                as AuthorizationRequestByReference
    }

    @Test
    fun `deserialize invalid url with no query`() {
        assertThrows<IllegalArgumentException> {
            AuthorizationRequestBaseClass.fromUriString("/authorize?")
        }

        assertThrows<IllegalArgumentException> {
            AuthorizationRequestBaseClass.fromUriString("/authorize")
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `deserialize invalid url with missing fields`() {
        val exampleUri = "/authorize?sdf=asd"
        assertThrows<MissingFieldException> {
            AuthorizationRequestBaseClass.fromUriString(exampleUri)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `deserialize invalid url with malformed query`() {
        val exampleUri = "/authorize?sad==&=&=&??="
        assertThrows<MissingFieldException> {
            AuthorizationRequestBaseClass.fromUriString(exampleUri)
        }
    }
}
