/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.statuslist

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class StatusListAggregation(vararg listUris: String) : Set<String> by setOf(*listUris) {

    fun serialize() =
        Json.encodeToString(
            JsonObject(mapOf(STATUS_LISTS_CLAIM to JsonArray(map { JsonPrimitive(it) })))
        )

    /**
     * Returns a `CachingCapableHttpStatusListSource` that is prepopulated with all elements of this
     * `StatusListAggregation`.
     */
    fun populatedStatusListSource(verifier: Verifier) =
        populatedStatusListSource(CachingCapableHttpStatusListSource(verifier, useCache = true))

    /**
     * Populates the given `CachingCapableHttpStatusListSource` with all elements of this
     * `StatusListAggregation`.
     */
    fun populatedStatusListSource(source: CachingCapableHttpStatusListSource): StatusListSource {
        forEach { source.get(URI.create(it)) }
        return source
    }

    companion object {

        const val STATUS_LISTS_CLAIM = "status_lists"

        fun fetch(
            aggregationUri: URI,
            client: HttpClient = defaultHttpClient,
            httpTimeout: Duration = Duration.ofSeconds(5)
        ): StatusListAggregation {
            val request =
                HttpRequest.newBuilder(aggregationUri)
                    .GET()
                    .header("accept", "application/json")
                    .timeout(httpTimeout)
                    .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() != 200) {
                fail(
                    StatusListException.Reason.INVALID_STATUS_LIST,
                    "Recieved ${response.statusCode()} response when fetching $aggregationUri"
                )
            }
            return parse(response.body())
        }

        fun parse(serialized: String): StatusListAggregation {
            val decoded = Json.decodeFromString<JsonObject>(serialized)
            require(decoded.size == 1 && decoded.containsKey(STATUS_LISTS_CLAIM)) {
                "Expected json object with single entry $STATUS_LISTS_CLAIM"
            }
            val listUris =
                decoded[STATUS_LISTS_CLAIM] as? JsonArray
                    ?: throw IllegalArgumentException("$STATUS_LISTS_CLAIM must be an array")
            val listUrisArray =
                listUris
                    .map {
                        val primitive =
                            it as? JsonPrimitive
                                ?: throw IllegalArgumentException(
                                    "$STATUS_LISTS_CLAIM element must be a string"
                                )
                        require(primitive.isString) {
                            "$STATUS_LISTS_CLAIM element must be a string"
                        }
                        primitive.content
                    }
                    .toTypedArray()
            return StatusListAggregation(*listUrisArray)
        }
    }
}
