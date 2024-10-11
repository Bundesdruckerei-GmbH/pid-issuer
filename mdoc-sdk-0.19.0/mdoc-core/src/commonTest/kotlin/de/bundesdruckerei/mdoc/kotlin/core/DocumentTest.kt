/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core

import COSE.Attribute
import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.auth.IssuerSigned
import de.bundesdruckerei.mdoc.kotlin.core.auth.IssuerSignedItem
import de.bundesdruckerei.mdoc.kotlin.core.auth.X5Chain
import de.bundesdruckerei.mdoc.kotlin.core.auth.dto.ValidityRange
import de.bundesdruckerei.mdoc.kotlin.core.common.toByteArray
import de.bundesdruckerei.mdoc.kotlin.core.common.toHex
import de.bundesdruckerei.mdoc.kotlin.sortIssuerSignedNameSpacesData
import de.bundesdruckerei.mdoc.kotlin.test.Data
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.security.KeyFactory
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneOffset
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class DocumentTest {

    private lateinit var issuerSignedCBOR: CBORObject
    private lateinit var deviceSignedCBOR: CBORObject
    private lateinit var errorsCBOR: CBORObject

    private lateinit var document: Document
    private val documentBytesWithIncompleteNamespaceItems =
        Data.Document.dataHexWithIncompleteNamespaceItems.toByteArray()

    private val rootCert =
        "MIICCDCCAa6gAwIBAgIVAKQdR/JBR5kqO1A58qwWP8EeeedkMAoGCCqGSM49BAMCMD0xCzAJBgNVBAYTAkRFMS4wLAYDVQQDDCVCRFIgSUFDQSBJU08vSUVDIDE4MDEzLTUgdjEgVEVTVC1PTkxZMB4XDTIzMDUyNTEzNTgxNloXDTMzMDUyNTEzNTgxNlowPTELMAkGA1UEBhMCREUxLjAsBgNVBAMMJUJEUiBJQUNBIElTTy9JRUMgMTgwMTMtNSB2MSBURVNULU9OTFkwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAATR2+QLT2jY0CLYfcb2D1kkGO+0702Hr6dOW1Vugo/BR0Y9O1ehY18zHXS2TKAxn4FZqu6P4PfryKaqfeysimXqo4GKMIGHMBIGA1UdEwEB/wQIMAYBAf8CAQAwDgYDVR0PAQH/BAQDAgEGMB0GA1UdEgQWMBSBEm1kbC1leGFtcGxlQGJkci5kZTAjBgNVHR8EHDAaMBigFqAUghJtZGwuZXhhbXBsZS5iZHIuZGUwHQYDVR0OBBYEFJeKVzSI/wkjO6Af3nmDm+EnKUBpMAoGCCqGSM49BAMCA0gAMEUCID8jhhvdybjgB9adCkQPB2e6tNPY7au5l/xZLQcvO4eGAiEA9naNuouhbVcHhPPRX4P1QU+e8KYpdDMntxl0jzo6Voc="
    private val sessionTranscriptBytes =
        "83D818588DA50063312E31018201D818584BA4010220012158201CCA8BE15F182FA62E7D4EE2F824093369180C4130C3BAD0884A94AFBE4B9999225820FAE9968977F9903AEB0F6766EE41E566D561D26CD99139EE1C2652741F4437230281830201A300F501F40A5042555DA3E22C4754A7A72B8119BE65C10581A363636174016474797065026744657461696C73F606F5D818584BA401022001215820BF683B6BD88713FBFD158F57278E9404158537529B475705CFD44C29AAB5AC6E225820D99A9FFA91103DE2D44BB5433D64638B6A4D2976625E8222EA71C50AA57CAD0BF6".toByteArray()

    // This key seems to be wrong
    private val dsKey =
        "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgTFekRUbYwtRBHzId" +
                "LZgUTpvoyNzsZO/68iY0Nx76MBmhRANCAAR7y73MzntlBH6HGrn8k8vsFfvOFUnB" +
                "bBrzc1FUIzpIfR0ZF/syjnS0INDIMMzw4JmPrHAfeI3dRgbcQVQ/IHnM"

    private val currentTimestamp =
        LocalDate.of(2024, Month.JANUARY, 18).atStartOfDay().atOffset(ZoneOffset.UTC)

    @Before
    fun setUp() {
        issuerSignedCBOR = Data.Document.cborObject["issuerSigned"]
        deviceSignedCBOR = Data.Document.cborObject["deviceSigned"]
        errorsCBOR = Data.Document.cborObject["errors"]

        document = Document.fromCBOR(Data.Document.cborObject)
    }

    @Test
    fun asCBOR() {
        val errorsCBORExpected = Data.Document.cborObject.get("errors")
        val issuerSignedCBORExpected = Data.Document.cborObject.get("issuerSigned")
        val issuerSignedCBORNameSpacesExpected: List<CBORObject>
        val deviceSignedCBORExpected = Data.Document.cborObject.get("deviceSigned")
        val docTypeCBORExpected = Data.Document.cborObject.get("docType").AsString()

        val errorsCBORActual = document.asCBOR().get("errors")
        val issuerSignedCBORActual = document.asCBOR().get("issuerSigned")
        val issuerSignedCBORNameSpacesActual: List<CBORObject>
        val deviceSignedCBORActual = document.asCBOR().get("deviceSigned")
        val docTypeCBORActual = document.asCBOR().get("docType").AsString()

        assertEquals(errorsCBORExpected, errorsCBORActual)

        assertEquals(
            issuerSignedCBORExpected.get("issuerAuth"),
            issuerSignedCBORActual.get("issuerAuth")
        )

        val result =
            sortIssuerSignedNameSpacesData(issuerSignedCBORExpected, issuerSignedCBORActual)
        issuerSignedCBORNameSpacesExpected = result[0]
        issuerSignedCBORNameSpacesActual = result[1]

        assertEquals(issuerSignedCBORNameSpacesExpected, issuerSignedCBORNameSpacesActual)

        assertEquals(deviceSignedCBORExpected, deviceSignedCBORActual)

        assertEquals(docTypeCBORExpected, docTypeCBORActual)
    }

    @Test
    fun getIssuerSigned() {
        val issuerSignedCBORExpected = issuerSignedCBOR
        val issuerSignedNameSpacesCBORExpected: List<CBORObject>

        val issuerSignedCBORActual = document.issuerSigned.asCBOR()
        val issuerSignedNameSpacesCBORActual: List<CBORObject>

        assertEquals(
            issuerSignedCBORExpected.get("issuerAuth"),
            issuerSignedCBORActual.get("issuerAuth")
        )

        val result =
            sortIssuerSignedNameSpacesData(issuerSignedCBORExpected, issuerSignedCBORActual)
        issuerSignedNameSpacesCBORExpected = result[0]
        issuerSignedNameSpacesCBORActual = result[1]

        assertEquals(issuerSignedNameSpacesCBORExpected, issuerSignedNameSpacesCBORActual)
    }

    @Test
    fun getDeviceSigned() {
        val deviceSigned = deviceSignedCBOR
        val deviceSignedCBOR = document.deviceSigned?.asCBOR()

        assertEquals(deviceSigned, deviceSignedCBOR)
    }

    @Test
    fun getErrors() {
        assertEquals(errorsCBOR, document.errors?.asCBOR())
    }

    @Test
    fun getDocType() {
        assertEquals(Data.mdlDocType, document.docType)
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun testBuildIssuerSigned() {
        val decodedDocument = Document.fromBytes(documentBytesWithIncompleteNamespaceItems)

        val x5Chain = X5Chain.fromCBOR(
            decodedDocument.issuerSigned.issuerAuth.findAttribute(
                CBORObject.FromObject(33),
                Attribute.UNPROTECTED
            )
        )

        val signingKey = KeyFactory.getInstance("EC")
            .generatePrivate(PKCS8EncodedKeySpec(Base64.decode(dsKey)))

        val docType = decodedDocument.docType

        val issuerSigned = IssuerSigned.Builder(docType, x5Chain).apply {
            decodedDocument.issuerSigned.nameSpaces?.forEach { (nameSpace, issuerSignedItems) ->
                issuerSignedItems.forEach { issuerSignedItem ->
                    putItem(
                        nameSpace = nameSpace,
                        identifier = issuerSignedItem.elementIdentifier,
                        value = issuerSignedItem.elementValue
                    )
                }
            }

            val mso = decodedDocument.issuerSigned.issuerAuth.mso

            validityRange = mso.validityInfo.run {
                ValidityRange(
                    validFrom = validFrom.toInstant(),
                    validUntil = validUntil.toInstant(),
                    expectedUpdate = expectedUpdate?.let { it.toInstant() }
                )
            }

            digestAlgorithm = mso.digestAlgorithm

            setDeviceKeyInfo(
                mso.deviceKeyInfo,
            )
        }.setNowOverride(Instant.parse("2024-01-01T10:15:30.00Z")).buildAndSign(signingKey)

        val validationResult = issuerSigned.validate(
            docType = docType,
            rootCertificates = listOf(
                CertificateFactory.getInstance("X509").generateCertificate(
                    ByteArrayInputStream(
                        Base64.decode(rootCert)
                    )
                ) as X509Certificate
            ),
            certificateValidator = { true },
            currentTimestamp = currentTimestamp
        )

        // CertPathValidator time cannot be overwritten or mocked
        Assert.assertFalse(validationResult.isValid())
        Assert.assertFalse(validationResult.hasValidCertificatePath)

        Assert.assertTrue(validationResult.hasValidValidityInfo)
        Assert.assertTrue(validationResult.hasValidDigests)
        Assert.assertTrue(validationResult.hasValidSignature)
        Assert.assertTrue(validationResult.hasValidDocType)
    }

    @Test
    fun testDocumentSerialisation() {
        val decoded = Document.fromBytes(documentBytesWithIncompleteNamespaceItems)
        val reencoded = decoded.asCBOR().EncodeToBytes()
        Assert.assertTrue(documentBytesWithIncompleteNamespaceItems.contentEquals(reencoded))
    }

    @Test
    fun testMobileeIDDocument() {
        val decoded = Document.fromBytes(documentBytesWithIncompleteNamespaceItems)
        val d0 = MobileeIDdocument("testdoc1", decoded.issuerSigned, null)
        val d1 = MobileeIDdocument("testdoc2", decoded.issuerSigned, null)
        val d2 = MobileeIDdocument("testdoc3", decoded.issuerSigned, null)
        val docs = arrayOf(d0, d1, d2)
        val encodedDocs = docs.asCBOR().EncodeToBytes()
        print(encodedDocs.toHex())
        val cborDocs = CBORObject.DecodeFromBytes(encodedDocs)
        val decodedDocs = mobileeIDDocumentsFromCBOR(cborDocs)
        Assert.assertTrue(
            decodedDocs[0].asCBOR().EncodeToBytes()
                .contentEquals(d0.asCBOR().EncodeToBytes())
        )
        Assert.assertTrue(
            decodedDocs[1].asCBOR().EncodeToBytes()
                .contentEquals(d1.asCBOR().EncodeToBytes())
        )
        Assert.assertTrue(
            decodedDocs[2].asCBOR().EncodeToBytes()
                .contentEquals(d2.asCBOR().EncodeToBytes())
        )
    }

    @Test
    fun testIsi() {
        val i =
            IssuerSignedItem.fromTaggedBytes(
                "D8185851A468646967657374494419022E6672616E646F6D5016F3AC07BC1CAE62ACB002C67F54015A71656C656D656E744964656E7469666965726B6167655F6F7665725F32316C656C656D656E7456616C7565F5".toByteArray()
            )
        println(i.asTaggedCBORBytes().EncodeToBytes().toHex())
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun testDocumentValidation() {
        val cf = CertificateFactory.getInstance("X509")
        val cert = cf.generateCertificate(
            ByteArrayInputStream(
                Base64.decode(rootCert)
            )
        ) as X509Certificate

        val sessionTranscript =
            SessionTranscript.fromCBOR(CBORObject.DecodeFromBytes(sessionTranscriptBytes))

        val mdoc = Document.fromBytes(documentBytesWithIncompleteNamespaceItems)


        // verify issuer signature
        val issuerAuthValidationResult = mdoc.issuerSigned.validate(
            docType = mdoc.docType,
            rootCertificates = listOf(cert),
            certificateValidator = {
                verifyCountryNamesInCertificates(
                    it[0],
                    it[1]
                ) && verifyStateNameInCertificates(
                    it[0],
                    it[1]
                )
            },
            currentTimestamp
        )
        // CertPathValidator time cannot be overwritten or mocked
        Assert.assertFalse(issuerAuthValidationResult.isValid())
        Assert.assertFalse(issuerAuthValidationResult.hasValidCertificatePath)

        Assert.assertTrue(issuerAuthValidationResult.hasValidValidityInfo)
        Assert.assertTrue(issuerAuthValidationResult.hasValidDigests)
        Assert.assertTrue(issuerAuthValidationResult.hasValidSignature)
        Assert.assertTrue(issuerAuthValidationResult.hasValidDocType)

        // verify device signature
        val deviceSigned = mdoc.deviceSigned?.validate(
            docType = mdoc.docType,
            deviceKeyInfo = mdoc.issuerSigned.issuerAuth.mso.deviceKeyInfo,
            readerMacKey = null,
            sessionTranscript = sessionTranscript,
        )
        Assert.assertNotNull(deviceSigned)
        Assert.assertTrue(deviceSigned!!.isValid())

        // or

        // verify issuer and device signature together
        val documentValidationResult = mdoc.validate(
            rootCertificates = listOf(cert),
            certificateValidator = {
                verifyCountryNamesInCertificates(
                    it[0],
                    it[1]
                ) && verifyStateNameInCertificates(
                    it[0],
                    it[1]
                )
            },
            readerMacKey = null,
            sessionTranscript = sessionTranscript,
            currentTimestamp = currentTimestamp,
        )

        // CertPathValidator time cannot be overwritten or mocked
        Assert.assertFalse(documentValidationResult.isValid())
        Assert.assertFalse(documentValidationResult.issuerAuthValidationResult.hasValidCertificatePath)

        Assert.assertTrue(documentValidationResult.issuerAuthValidationResult.hasValidValidityInfo)
        Assert.assertTrue(documentValidationResult.issuerAuthValidationResult.hasValidDigests)
        Assert.assertTrue(documentValidationResult.issuerAuthValidationResult.hasValidSignature)
        Assert.assertTrue(documentValidationResult.issuerAuthValidationResult.hasValidDocType)
        Assert.assertTrue(documentValidationResult.deviceAuthValidationResult.isValid())

        mdoc.issuerSigned.nameSpaces?.entries?.forEach {
            println("${it.key} (${it.value.size} entries)")
            it.value.forEach {
                println("\t ${it.elementIdentifier}: ${it.elementValue}")
            }
        }
    }

    /**
     * Method for verifying that the country name is the same in both IACA and DS certificate
     *
     * @param iacaCert X509Certificate
     * @param dsCert X509Certificate
     * @return true if the country names are the same or false if they are not the same
     */
    fun verifyCountryNamesInCertificates(
        iacaCert: X509Certificate,
        dsCert: X509Certificate
    ): Boolean {
        val iacaCountryNames = hashSetOf<String>()
        val dsCountryNames = hashSetOf<String>()

        try {
            val iacaCountryNameRDN =
                X500Name(iacaCert.subjectX500Principal.name).getRDNs(BCStyle.C)
            iacaCountryNameRDN.forEach { name ->
                iacaCountryNames.add(name.first.value.toString())
            }
        } catch (e: Exception) {
            println("IACA certificate doesn't have country name within its subject field!")
        }

        try {
            val dsCountryNameRDN =
                X500Name(dsCert.subjectX500Principal.name).getRDNs(BCStyle.C)
            dsCountryNameRDN.forEach { name ->
                dsCountryNames.add(name.first.value.toString())
            }
        } catch (e: Exception) {
            println("DS certificate doesn't have country name within its subject field!")
        }

        if (iacaCountryNames.isEmpty()) {
            println("CountryName in IACA certificate must not be empty!")
            return false
        }

        if (dsCountryNames.isEmpty()) {
            println("CountryName in DS certificate must not be empty!")
            return false
        }

        return iacaCountryNames == dsCountryNames
    }

    /**
     * Method for verifying that the state or province name is the same in both IACA and DS certificate
     *
     * @param iacaCert X509Certificate
     * @param dsCert X509Certificate
     * @return true if the country names are the same or false if they are not the same
     */
    fun verifyStateNameInCertificates(
        iacaCert: X509Certificate,
        dsCert: X509Certificate
    ): Boolean {
        val iacaStateNameRDN =
            X500Name(iacaCert.subjectX500Principal.name).getRDNs(BCStyle.ST)
        val dsStateNameRDN = X500Name(dsCert.subjectX500Principal.name).getRDNs(BCStyle.ST)

        val iacaStateNames = hashSetOf<String>()
        val dsStateNames = hashSetOf<String>()

        iacaStateNameRDN.forEach { name ->
            iacaStateNames.add(name.first.value.toString())
        }

        dsStateNameRDN.forEach { name ->
            dsStateNames.add(name.first.value.toString())
        }

        return iacaStateNames == dsStateNames
    }
}
