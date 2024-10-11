/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.vci.credentials.sdjwt

import de.bdr.openid4vc.common.signing.Signer
import eu.europa.ec.eudi.sdjwt.SdObject
import eu.europa.ec.eudi.sdjwt.plain
import eu.europa.ec.eudi.sdjwt.sd
import eu.europa.ec.eudi.sdjwt.sdJwt
import java.util.*
import kotlinx.serialization.json.*

abstract class SimpleSdJwtVcCreator(
    issuer: String,
    configuration: SdJwtVcCredentialConfiguration,
    signer: Signer
) : SdJwtVcCredentialCreator(issuer, configuration, signer) {

    companion object {
        val PLAIN_KEYS = setOf("cnf", "exp", "iat", "iss", "nbf", "status", "vct")
    }

    private fun toJsonElement(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull
            is Map<*, *> ->
                JsonObject(
                    value.mapKeys { it.key.toString() }.mapValues { toJsonElement(it.value) }
                )
            is Number -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is List<*> -> JsonArray(value.map { toJsonElement(it) })
            else ->
                error(
                    "Invalid entry in claims. Only Number, String, Boolean and null are allowed and List<*> and Map<String,*> of those types and themselves."
                )
        }
    }

    final override fun createSdObject(issuanceId: UUID): SdObject {
        val claims = create(issuanceId)

        val (plainKeys, sdKeys) = claims.keys.partition { PLAIN_KEYS.contains(it) }

        return sdJwt {
            plain(toJsonElement(claims.filter { plainKeys.contains(it.key) }))
            sd(toJsonElement(claims.filter { sdKeys.contains(it.key) }))
        }
    }

    /**
     * Creates the claims to put in an SD-JWT VC for a given [CredentialData] instance. All root
     * claims will be selectively disclosable, expect the keys defined in [PLAIN_KEYS].
     *
     * @return claims.
     */
    abstract fun create(issuanceId: UUID): Map<String, Any>
}
