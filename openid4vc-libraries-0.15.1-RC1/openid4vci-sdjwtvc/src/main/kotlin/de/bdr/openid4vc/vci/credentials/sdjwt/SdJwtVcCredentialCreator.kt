/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.vci.credentials.sdjwt

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.jwk.JWK
import de.bdr.openid4vc.common.Algorithm
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialDescription
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialRequest
import de.bdr.openid4vc.common.mapStructureToJson
import de.bdr.openid4vc.common.signing.Signer
import de.bdr.openid4vc.common.vci.Credential
import de.bdr.openid4vc.common.vci.CredentialRequest
import de.bdr.openid4vc.vci.credentials.CredentialCreator
import de.bdr.openid4vc.vci.service.statuslist.StatusReference
import de.bdr.openid4vc.vci.utils.clock
import eu.europa.ec.eudi.sdjwt.*
import java.time.Instant
import java.util.*
import kotlinx.serialization.json.*

/** Adapter from [SdJwtVcComplexCreator] to [CredentialCreator]. */
abstract class SdJwtVcCredentialCreator(
    private val issuer: String,
    configuration: SdJwtVcCredentialConfiguration,
    private val signer: Signer,
) : CredentialCreator() {

    override val configuration = configuration

    override fun getCredentialDescription(displayMessages: Map<Locale, Map<String, Any>>?) =
        SdJwtVcCredentialDescription(
            scope = configuration.id,
            display = buildCredentialDisplayList(displayMessages, configuration.id),
            vct = configuration.vct,
            claims = buildClaimsDisplay(displayMessages),
            cryptographicBindingMethodsSupported = listOf("jwk"),
            cryptographicSigningAlgValuesSupported =
                listOf(
                    when (signer.algorithm) {
                        Algorithm.ES256 -> "ES256"
                        Algorithm.ES384 -> "ES384"
                        Algorithm.ES512 -> "ES512"
                        Algorithm.DVS_P256_SHA256_HS256 -> "DVS-P256-SHA256-HS256"
                    }
                ),
            proofTypesSupported = if (configuration.keyBinding) emptyMap() else null,
        )

    override val signers = listOf(signer)

    private fun buildCredentialDisplayList(
        displayMessages: Map<Locale, Map<String, Any>>?,
        credentialConfigurationId: String,
    ): List<JsonObject>? {
        val credentialDisplayList = mutableListOf<Map<String, Any>>()
        displayMessages?.forEach { (locale, localeDisplay) ->
            localeDisplay
                .getPath(
                    "credential_configurations_supported",
                    credentialConfigurationId,
                    "display",
                )
                ?.toMutableMap()
                ?.let { props ->
                    props["locale"] = locale.toLanguageTag()
                    credentialDisplayList.add(props.toMap())
                }
        }
        return if (credentialDisplayList.isEmpty()) null
        else credentialDisplayList.map { it.mapStructureToJson() }
    }

    private fun buildClaimsDisplay(displayMessages: Map<Locale, Map<String, Any>>?): JsonObject? {
        val claimsDisplays = mutableMapOf<String, Any>()
        displayMessages?.forEach { (locale, prop) ->
            prop
                .getPath(
                    "credentials_supported",
                    configuration.id,
                    "credential_definition",
                    "claims",
                )
                ?.let { props ->
                    props.forEach { (claimName, display) ->
                        @SuppressWarnings("UNCHECKED_CAST")
                        val claimDisplay =
                            claimsDisplays.getOrPut(claimName) { mutableMapOf<String, Any>() }
                                as MutableMap<String, Any>
                        @SuppressWarnings("UNCHECKED_CAST")
                        val claimDisplayList =
                            claimDisplay.getOrPut("display") { mutableListOf<Any>() }
                                as MutableList<Any>
                        claimDisplayList.add(
                            mapOf("locale" to locale.toLanguageTag(), "name" to display)
                        )
                    }
                }
        }
        return if (claimsDisplays.isEmpty()) null else claimsDisplays.mapStructureToJson()
    }

    override fun validateCredentialRequest(request: CredentialRequest): Boolean {
        if (request !is SdJwtVcCredentialRequest) return false
        return request.vct == configuration.vct
    }

    final override fun create(
        request: CredentialRequest,
        issuanceId: UUID,
        holderBindingKey: JWK?,
        status: StatusReference?,
    ): Credential {

        val sdJwtSigner = SdJwtSigner(signer, configuration.jadesSignatures)

        val issuer =
            SdJwtIssuer.nimbus(
                sdJwtFactory =
                    SdJwtFactory.of(
                        hashAlgorithm = HashAlgorithm.SHA_256,
                        fallbackMinimumDigests =
                            if (configuration.numOfDecoysLimit < 1) null
                            else configuration.numOfDecoysLimit,
                    ),
                signer = sdJwtSigner.signer(),
                signAlgorithm = sdJwtSigner.signer().jwsAlgorithm,
                jwsHeaderCustomization = {
                    sdJwtSigner.customizeHeader(this)
                    type(JOSEObjectType("vc+sd-jwt"))
                },
            )

        val sdObject = createSdObject(issuanceId)

        val sdJwtSpec = sdObject.addDefaultClaims(holderBindingKey, status)

        return Credential(issuer.issue(sdJwtSpec).getOrThrow().serialize())
    }

    private fun SdObject.addDefaultClaims(
        holderBindingKey: JWK?,
        status: StatusReference?,
    ): SdObject {

        val iat: Long =
            ((((this["iat"] as? SdObjectElement.Disclosable)?.disclosable
                        as? DisclosableJsonElement.Plain)
                    ?.value as? JsonPrimitive)
                ?.long) ?: (Instant.now(clock).toEpochMilli() / 1000)

        if (status != null) {
            check(!this.contains("status")) {
                "A status list is configured for credential ${configuration.id} but a status claim was already present"
            }
        }

        val defaultClaims = sdJwt {
            plain {
                put("vct", configuration.vct)
                iss(issuer)
                iat(iat)
                configuration.lifetime?.let { exp(iat + it.toSeconds()) }
                if (status != null) {
                    putJsonObject("status") {
                        putJsonObject("status_list") {
                            put("uri", status.uri)
                            put("idx", status.index)
                        }
                    }
                }
            }
            if (holderBindingKey != null) cnf(holderBindingKey)
        }

        // order is important already set values override defaultClaims
        return defaultClaims.plus(this)
    }

    abstract fun createSdObject(issuanceId: UUID): SdObject

    override fun onStatusListEntriesUsed(
        issuanceId: UUID,
        indicesByListUri: Map<String, Collection<Int>>,
    ) {
        // to be overridden if needed
    }
}
