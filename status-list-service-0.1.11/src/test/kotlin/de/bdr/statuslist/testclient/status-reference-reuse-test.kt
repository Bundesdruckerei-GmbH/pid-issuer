/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.testclient

import java.time.Instant
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

/**
 * Retrieves indices from the service and detects if indices were used twice.
 *
 * This can be used together with crashing-redis-loop.sh, that creates a permamently crashing redis
 * instance.
 */
fun main() {
    val client = RestClient.create()
    val seen = mutableSetOf<Reference>()
    var errors = 0
    while (true) {
        try {
            val reference =
                client
                    .post()
                    .uri("http://localhost:8090/api/tests/new-reference")
                    .header("x-api-key", "af05bedc-ec26-472a-af56-f3b862e8e00d")
                    .retrieve()
                    .body(Reference::class.java) ?: continue
            if (!seen.add(reference)) {
                println("${Instant.now()} - INDEX USED TWICE")
                System.exit(1)
            }
        } catch (e: RestClientResponseException) {
            errors++
        }
        if ((seen.size + errors) % 1000 == 0)
            println("${Instant.now()} - ${seen.size} seen, $errors errors")
        Thread.sleep(1)
    }
}

data class Reference(val uri: String, val index: Int)
