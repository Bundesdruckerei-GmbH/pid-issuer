/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.vci.credentials.mdoc

import COSE.AlgorithmID
import COSE.OneKey
import COSE.sign
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.jwk.RSAKey
import com.upokecenter.cbor.CBORObject
import de.bdr.openid4vc.common.Algorithm
import de.bdr.openid4vc.common.Algorithm.*
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialDescription
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialRequest
import de.bdr.openid4vc.common.formats.msomdoc.Policy
import de.bdr.openid4vc.common.mapStructureToJson
import de.bdr.openid4vc.common.signing.Signer
import de.bdr.openid4vc.common.signing.X509KeyMaterial
import de.bdr.openid4vc.common.vci.CredentialRequest
import de.bdr.openid4vc.vci.credentials.CredentialCreator
import de.bdr.openid4vc.vci.service.statuslist.StatusReference
import de.bdr.openid4vc.vci.utils.clock
import de.bundesdruckerei.mdoc.kotlin.core.Document
import de.bundesdruckerei.mdoc.kotlin.core.auth.DeviceKeyInfo
import de.bundesdruckerei.mdoc.kotlin.core.auth.IssuerSigned
import de.bundesdruckerei.mdoc.kotlin.core.auth.X5Chain
import de.bundesdruckerei.mdoc.kotlin.core.auth.dto.ValidityRange
import java.security.PublicKey
import java.time.Instant
import java.util.*
import kotlinx.serialization.json.JsonObject
import org.bouncycastle.asn1.bsi.BSIObjectIdentifiers.algorithm

abstract class MDocCredentialCreator(
    configuration: MDocCredentialConfiguration,
    private val signer: Signer
) : CredentialCreator() {

    override val configuration = configuration

    private val x5Chain: X5Chain

    init {
        val keys = signer.keys

        if (keys !is X509KeyMaterial) {
            throw IllegalArgumentException(
                "MDocCredentialCreator requires a signer that provides X509KeyMaterial"
            )
        }

        x5Chain =
            when (keys.certificates.size) {
                0 -> throw IllegalStateException("Empty certificates not allowed")
                1 -> X5Chain.of(keys.certificates[0])
                2 -> X5Chain.of(keys.certificates[0], keys.certificates[1])
                else ->
                    X5Chain.of(
                        keys.certificates[0],
                        keys.certificates[1],
                        *keys.certificates.subList(2, keys.certificates.size).toTypedArray()
                    )
            }
    }

    override val signers = listOf(signer)

    private fun buildCredentialDisplayList(
        displayMessages: Map<Locale, Map<String, Any>>?,
        credentialConfigurationId: String
    ): List<JsonObject>? {
        val credentialDisplayList = mutableListOf<Map<String, Any>>()
        displayMessages?.forEach { (locale, localeDisplay) ->
            localeDisplay
                .getPath(
                    "credential_configurations_supported",
                    credentialConfigurationId,
                    "display"
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

    override fun getCredentialDescription(display: Map<Locale, Map<String, Any>>?) =
        MsoMdocCredentialDescription(
            scope = configuration.id,
            doctype = configuration.docType,
            display = buildCredentialDisplayList(display, configuration.id),
            policy =
                if (configuration.batchSize == null) null
                else Policy(batchSize = configuration.batchSize!!, oneTimeUse = true),
            cryptographicBindingMethodsSupported = listOf("cose_key"),
            cryptographicSigningAlgValuesSupported =
                listOf(
                    when (signer.algorithm) {
                        ES256 -> "ES256"
                        ES384 -> "ES384"
                        ES512 -> "ES512"
                        DVS_P256_SHA256_HS256 ->
                            error("Algorithm not supported ${signer.algorithm}")
                    }
                ),
            credentialAlgValuesSupported = listOf(-7, -36, -37),
            credentialCrvValuesSupported = listOf(1, 2, 3),
            proofTypesSupported = if (configuration.keyBinding) emptyMap() else null
        )

    override fun validateCredentialRequest(request: CredentialRequest): Boolean {
        if (request !is MsoMdocCredentialRequest) return false
        return request.doctype == configuration.docType
    }

    override fun create(
        request: CredentialRequest,
        issuanceId: UUID,
        holderBindingKey: JWK?,
        status: StatusReference?
    ): String {
        checkNotNull(holderBindingKey) {
            throw IllegalStateException("Holder binding key is required")
        }

        val mdocData = create(issuanceId)
        val mdocCbor =
            Document(
                    docType = configuration.docType,
                    issuerSigned =
                        IssuerSigned.build(configuration.docType, x5Chain) {
                                deviceKeyInfo =
                                    DeviceKeyInfo(OneKey(holderBindingKey.toPublicKey(), null))
                                putNameSpaces(
                                    mdocData.namespaces.mapValues { dataElements ->
                                        dataElements.value.mapValues {
                                            CBORObject.FromObject(it.value)
                                        }
                                    }
                                )
                                signingAlgorithm = signer.algorithm.toCoseAlgorithmId()
                                val validFrom = mdocData.validFrom ?: Instant.now(clock)
                                val validUntil =
                                    mdocData.validUntil ?: validFrom.plus(configuration.lifetime)
                                validityRange = ValidityRange(validFrom, validUntil)
                            }
                            .apply { issuerAuth.sign(signer) }
                )
                .asCBOR()

        val mdocBytes =
            when (configuration.credentialStructure) {
                CredentialStructure.DOCUMENT -> mdocCbor.EncodeToBytes()
                CredentialStructure.ISSUER_SIGNED -> mdocCbor.get("issuerSigned").EncodeToBytes()
            }

        return Base64.getUrlEncoder().withoutPadding().encodeToString(mdocBytes)
    }

    abstract fun create(issuanceId: UUID): MdocData

    private fun Algorithm.toCoseAlgorithmId() =
        when (this) {
            ES256 -> AlgorithmID.ECDSA_256
            ES384 -> AlgorithmID.ECDSA_384
            ES512 -> AlgorithmID.ECDSA_512
            DVS_P256_SHA256_HS256 -> error("Unsupported algorithm ${this::class.qualifiedName}")
        }

    private fun JWK.toPublicKey(): PublicKey {
        return when (this) {
            is ECKey -> toPublicKey()
            is RSAKey -> toPublicKey()
            is OctetKeyPair -> toPublicKey()
            else -> error("Unsupported key type ${this::class.qualifiedName}")
        }
    }

    override fun onStatusListEntriesUsed(
        issuanceId: UUID,
        indicesByListUri: Map<String, Collection<Int>>
    ) {
        // to be overridden if needed
    }
}
