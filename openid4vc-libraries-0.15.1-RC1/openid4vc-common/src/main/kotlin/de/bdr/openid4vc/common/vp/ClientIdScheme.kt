/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp

enum class ClientIdScheme(val value: String, val impliesSignedRequest: Boolean) {
    REDIRECT_URI("redirect_uri", false),
    X509_SAN_DNS("x509_san_dns", true),
    X509_SAN_URI("x509_san_uri", true),
    PRE_REGISTERED("", false);

    companion object {

        private const val DELIMITER = ':'

        fun fromClientId(clientId: String): Pair<ClientIdScheme, String> {

            listOf(REDIRECT_URI, X509_SAN_DNS, X509_SAN_URI)
                .find { scheme -> clientId.startsWith("${scheme.value}${DELIMITER}") }
                ?.let {
                    return Pair(it, clientId.removePrefix("${it.value}${DELIMITER}"))
                }

            // if no prefix matches, check pre_registered scheme (no colon character)
            if (!clientId.contains(DELIMITER)) return Pair(PRE_REGISTERED, clientId)

            // otherwise throw exception
            throw IllegalArgumentException(
                "Unable to get Client Identifier Scheme from Client ID string "
            )
        }
    }
}
