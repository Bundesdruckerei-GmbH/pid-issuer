/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.formats.msomdoc

import com.upokecenter.cbor.CBORObject
import de.bdr.openid4vc.common.credentials.IssuerInfo
import de.bdr.openid4vc.common.credentials.StatusInfo
import de.bdr.openid4vc.common.vp.dcql.DistinctClaimsPathPointer
import de.bdr.openid4vc.common.vp.dcql.ObjectElementSelector
import de.bundesdruckerei.mdoc.kotlin.core.Document
import de.bundesdruckerei.mdoc.kotlin.core.auth.IssuerSigned
import javax.security.auth.x500.X500Principal

class MdocCoreMsoMdocCredential
private constructor(
    documentBytes: ByteArray? = null,
    documentCbor: CBORObject? = null,
    val document: Document? = null,
    issuerSignedBytes: ByteArray? = null,
    issuerSignedCbor: CBORObject? = null,
    val issuerSigned: IssuerSigned,
    override val issuer: IssuerInfo,
    override val status: StatusInfo? = null,
) : MsoMdocCredential {

    companion object {

        fun fromDocumentBytes(bytes: ByteArray): MdocCoreMsoMdocCredential {
            val cbor = CBORObject.DecodeFromBytes(bytes)
            val document = Document.fromCBOR(cbor)
            return MdocCoreMsoMdocCredential(
                documentBytes = bytes,
                documentCbor = cbor,
                document = document,
                issuerSigned = document.issuerSigned,
                issuer = document.issuerSigned.issuerInfo(),
            )
        }

        fun fromDocumentCBOR(cbor: CBORObject): MdocCoreMsoMdocCredential {
            val document = Document.fromCBOR(cbor)
            return MdocCoreMsoMdocCredential(
                documentCbor = cbor,
                document = document,
                issuerSigned = document.issuerSigned,
                issuer = document.issuerSigned.issuerInfo(),
            )
        }

        fun fromDocument(document: Document): MdocCoreMsoMdocCredential {
            return MdocCoreMsoMdocCredential(
                document = document,
                issuerSigned = document.issuerSigned,
                issuer = document.issuerSigned.issuerInfo(),
            )
        }

        fun fromIssuerSignedBytes(bytes: ByteArray): MdocCoreMsoMdocCredential {
            val cbor = CBORObject.DecodeFromBytes(bytes)
            val issuerSigned = IssuerSigned.fromCBOR(cbor)
            return MdocCoreMsoMdocCredential(
                issuerSignedBytes = bytes,
                issuerSignedCbor = cbor,
                issuerSigned = issuerSigned,
                issuer = issuerSigned.issuerInfo(),
            )
        }

        fun fromIssuerSignedCBOR(cbor: CBORObject): MdocCoreMsoMdocCredential {
            val issuerSigned = IssuerSigned.fromCBOR(cbor)
            return MdocCoreMsoMdocCredential(
                issuerSignedCbor = cbor,
                issuerSigned = issuerSigned,
                issuer = issuerSigned.issuerInfo(),
            )
        }

        fun fromIssuerSigned(issuerSigned: IssuerSigned): MdocCoreMsoMdocCredential {
            return MdocCoreMsoMdocCredential(
                issuerSigned = issuerSigned,
                issuer = issuerSigned.issuerInfo(),
            )
        }

        private fun IssuerSigned.issuerInfo(): IssuerInfo {
            val cert = issuerAuth.obtainX5Chain().endEntityCert
            return IssuerInfo(
                identifier = cert.subjectX500Principal.getName(X500Principal.RFC2253),
                x509Certificate = cert,
            )
        }
    }

    override val format = MsoMdocCredentialFormat

    val issuerSignedBytes: ByteArray by lazy {
        when {
            issuerSignedBytes != null -> issuerSignedBytes
            else -> this.issuerSignedCbor.EncodeToBytes()
        }
    }

    val issuerSignedCbor: CBORObject by lazy {
        when {
            issuerSignedCbor != null -> issuerSignedCbor
            else -> issuerSigned.asCBOR()
        }
    }

    val documentBytes: ByteArray? by lazy {
        when {
            documentBytes != null -> documentBytes
            else -> this.documentCbor?.EncodeToBytes()
        }
    }

    val documentCbor: CBORObject? by lazy {
        when {
            documentCbor != null -> documentCbor
            else -> document?.asCBOR()
        }
    }

    override fun namespacesAndValues(
        toDisclose: Set<DistinctClaimsPathPointer>
    ): Map<String, Map<String, CBORObject>> {
        return namespacesAndValues
            .filterKeys { key -> toDisclose.any { it.startsWith(key) } }
            .mapValues { namespacesAndValues ->
                namespacesAndValues.value.filterKeys { key ->
                    toDisclose.any { it.startsWith(namespacesAndValues.key, key) }
                }
            }
    }

    private fun DistinctClaimsPathPointer.startsWith(vararg names: String): Boolean {
        if (selectors.size < names.size) return false
        names.forEachIndexed { index, name ->
            val claimName = (selectors[index] as? ObjectElementSelector)?.claimName
            if (claimName != name) return false
        }
        return true
    }

    override fun withStatus(status: StatusInfo) =
        MdocCoreMsoMdocCredential(
            documentBytes,
            documentCbor,
            document,
            issuerSignedBytes,
            issuerSignedCbor,
            issuerSigned,
            issuer,
            status,
        )

    override fun withIssuer(issuer: IssuerInfo) =
        MdocCoreMsoMdocCredential(
            documentBytes,
            documentCbor,
            document,
            issuerSignedBytes,
            issuerSignedCbor,
            issuerSigned,
            issuer,
            status,
        )

    override val doctype by lazy { issuerSigned.issuerAuth.mso.docType }

    override val namespacesAndValues by lazy {
        issuerSigned.nameSpaces?.mapValues {
            it.value.associate { Pair(it.elementIdentifier, it.elementValue) }
        } ?: emptyMap()
    }
}
