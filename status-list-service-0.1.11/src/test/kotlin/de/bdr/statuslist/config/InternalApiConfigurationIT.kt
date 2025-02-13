/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.config

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = ["spring.profiles.include=docker,api-port"],
)
class InternalApiConfigurationIT(@Autowired val restTemplate: TestRestTemplate) {
    private val apiKey = "366A9069-2965-4667-9AD2-5C51D71046D8"
    private val listIdentifier = "verified-email"

    @Test
    fun `should not get reference on default port`() {
        val headers = HttpHeaders()
        headers.set("x-api-key", apiKey)
        val response =
            restTemplate.exchange(
                "/pools/$listIdentifier/new-references",
                HttpMethod.POST,
                HttpEntity<String>(null.toString()),
                String::class.java,
            )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }
}
