/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.vci.service

/** Represents an HTTP request. */
class HttpRequest<T : Any>(
    /** The request method, e.g. GET,POST,PUT,DELETE,... */
    val method: String,
    /** The request uri */
    val uri: String,
    /** The path component of the request uri */
    val path: String,
    /** The request headers, keys are lowercased */
    val headers: HttpHeaders,
    /** The request parameters. This map is a combination of URI query parameters and form data. */
    val parameters: Map<String, String>,
    /** The body of the http request, if any */
    val body: T,
) {

    companion object {

        fun bodyless(
            method: String,
            uri: String,
            path: String,
            headers: HttpHeaders,
            parameters: Map<String, String>,
        ): HttpRequest<Unit> = HttpRequest(method, uri, path, headers, parameters, Unit)

        fun textual(
            method: String,
            uri: String,
            path: String,
            headers: HttpHeaders,
            parameters: Map<String, String>,
            body: String,
        ): HttpRequest<String> = HttpRequest(method, uri, path, headers, parameters, body)
    }
}

class HttpResponse(val status: Int, val headers: HttpHeaders, val body: String?) {
    fun doThrow(): Nothing {
        throw HttpResponseException(this)
    }
}

/** A map of HTTP headers. */
class HttpHeaders(headers: Map<String, List<String>>) {

    companion object {
        fun of(headers: Map<String, String>) = HttpHeaders(headers.mapValues { listOf(it.value) })

        fun empty() = HttpHeaders(emptyMap())
    }

    constructor(
        vararg headers: Pair<String, String>
    ) : this(mapOf(*headers).mapValues { listOf(it.value) })

    /** The headers, keys are lowercased */
    val headers = headers.mapKeys { it.key.lowercase() }

    /** Gets the first header value for a key, case-insensitive. */
    operator fun get(key: String) = headers[key.lowercase()]?.firstOrNull()
}

open class HttpResponseException(val response: HttpResponse) : Exception()

val CONTENT_TYPE_APPLICATION_JSON = Pair("content-type", "application/json")
val CONTENT_TYPE_APPLICATION_JWT = Pair("content-type", "application/jwt")
