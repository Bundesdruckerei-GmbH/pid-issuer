/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.crypto

import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBORType
import de.bundesdruckerei.mdoc.kotlin.core.auth.IssuerAuth
import de.bundesdruckerei.mdoc.kotlin.core.auth.toCertPath
import de.bundesdruckerei.mdoc.kotlin.core.common.log
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.ASN1Primitive
import org.bouncycastle.asn1.x509.*
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.util.encoders.Hex
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.cert.*
import java.util.*

object CertUtils {

    /**
     * Method for performing certificate path validation acc. to the ISO 18013-5 standard
     *
     * @param iacaCert root certificate which will be used as trust anchor
     * @param dsCert target certificate
     * @param certPath list which contains target and optional intermediate certificates
     * @param certStore optional list which contains CRLs
     * @return CertificatePathValidationResult object
     */
    fun validateCertificateChain(
        iacaCert: X509Certificate,
        dsCert: X509Certificate,
        certPath: CertPath,
        certStore: CertStore? = null
    ): Boolean {
        // load IACA certificate into the KeyStore
        val trustStore = KeyStore.getInstance("PKCS12")
        trustStore.load(null, "".toCharArray())
        trustStore.setCertificateEntry("IACA", iacaCert)

        val certPathValidator = CertPathValidator.getInstance("PKIX")
        val validationParameters = PKIXParameters(trustStore)

        // extensions validation
        val extensions = getProperExtensionsForValidation(iacaCert, dsCert, certPath)
        validationParameters.targetCertConstraints = extensions

        // CRL validation
        val revocationChecker = certPathValidator.revocationChecker as PKIXRevocationChecker
        revocationChecker.options = EnumSet.of(
            PKIXRevocationChecker.Option.PREFER_CRLS, // prefer CLR over OCSP
            PKIXRevocationChecker.Option.ONLY_END_ENTITY,
            PKIXRevocationChecker.Option.SOFT_FAIL, // without this, CRL will fail
            PKIXRevocationChecker.Option.NO_FALLBACK // don't fall back to OCSP checking
        )

        validationParameters.addCertPathChecker(revocationChecker)

        // if CRL is present, add it to the validation parameters
        certStore?.let {
            validationParameters.certStores = listOf(it)
        }

        try {
            val result = certPathValidator.validate(
                certPath,
                validationParameters
            ) as PKIXCertPathValidatorResult
            log.d(
                "Certificate chain is valid! Issuer name: ${result.trustAnchor.trustedCert.issuerX500Principal.name}"
            )
        } catch (ex: Exception) {
            log.e("Certificate chain is not valid! Exception: ${ex.message}")
            return false
        }

        return true
    }

    /**
     * Method for getting proper extensions (acc. to ISO 18013-5 Annex B DS profile)
     * which needs to be checked against DS (target) certificate
     *
     * @param iacaCert root X509Certificate
     * @param dsCert target X509Certificate
     * @return extensions as X509CertSelector
     */
    private fun getProperExtensionsForValidation(
        iacaCert: X509Certificate,
        dsCert: X509Certificate,
        certPath: CertPath
    ): X509CertSelector {
        val extensionsSelector = X509CertSelector()

        // -------------------CRITICAL EXTENSIONS---------------------
        /*
            NOTE:

            False in boolean array for key usage doesn't mean that that values needs to be 0
            but rather that that values is not checked and that it can be either 0 or 1 and every case will be valid
         */
        extensionsSelector.keyUsage =
            booleanArrayOf(true, false, false, false, false, false, false, false, false)
        extensionsSelector.extendedKeyUsage = setOf("1.0.18013.5.1.2")

        // -----------------NON-CRITICAL EXTENSIONS-------------------
        val input = ASN1InputStream(dsCert.tbsCertificate)
        val certificate = TBSCertificate.getInstance(input.readObject())

        // Subject key identifier extension
        val publicKeyData = certificate.subjectPublicKeyInfo.publicKeyData.bytes
        val publicKeyBytes = CryptoUtils.digest(publicKeyData, "SHA-1")
        // generate SubjectKeyIdentifier sequence
        val subjectKeyIdentifier = SubjectKeyIdentifier(publicKeyBytes)

        extensionsSelector.subjectKeyIdentifier = subjectKeyIdentifier.encoded

        // Authority key identifier extension
        // get the conforming CA which signed DS certificate
        val caCert: X509Certificate = if (certPath.certificates.size > 1) {
            certPath.certificates[1] as X509Certificate
        } else {
            iacaCert
        }

        val subjectKeyIdentifierEncoded =
            caCert.getExtensionValue(Extension.subjectKeyIdentifier.id)
        // Unwrap first 'layer'
        val skiPrimitive: ASN1Primitive? = subjectKeyIdentifierEncoded?.let {
            JcaX509ExtensionUtils.parseExtensionValue(it)
        }
        // Unwrap second 'layer'
        val keyIdentifier: ByteArray? =
            skiPrimitive?.let { ASN1OctetString.getInstance(it.encoded).octets }
        // generate AuthorityKeyIdentifier sequence
        val authorityKeyIdentifier = AuthorityKeyIdentifier(keyIdentifier)

        extensionsSelector.authorityKeyIdentifier = authorityKeyIdentifier.encoded

        // CRL Distribution Points
        val crlDistributionPointEncoded =
            dsCert.getExtensionValue(Extension.cRLDistributionPoints.id)
        var crlDPPrimitive: ASN1Primitive? = null
        crlDistributionPointEncoded?.let {
            crlDPPrimitive = JcaX509ExtensionUtils.parseExtensionValue(crlDistributionPointEncoded)
        }
        var distPoints: Array<DistributionPoint>? = null
        crlDPPrimitive?.let {
            distPoints = CRLDistPoint.getInstance(crlDPPrimitive).distributionPoints
        }

        distPoints?.forEach {
            checkNotNull(it.distributionPoint.name)
        }

        return extensionsSelector
    }

    @Deprecated(
        "Use the new X5Chain interface",
        replaceWith = ReplaceWith(
            "IssuerAuth.obtainX5Chain(CBORObject).toCertPath()"
        )
    )
    fun getCertificateChainFromIssuerAuth(issuerAuthCBORObject: CBORObject) = IssuerAuth.obtainX5Chain(issuerAuthCBORObject).toCertPath()

    fun decodeCertificates(bytes: ByteArray): CertPath {
        val cf = CertificateFactory.getInstance("X509")
        var input: ByteArrayInputStream
        val x509cert: X509Certificate
        var certPath: CertPath

        try {
            input = ByteArrayInputStream(bytes)

            val certificates = cf.generateCertificates(input).toList()

            certPath = cf.generateCertPath(certificates)
        } catch (_: Exception) {
            val cert =
                "-----BEGIN CERTIFICATE-----\n" + String(bytes) + "\n-----END CERTIFICATE-----"
            input = ByteArrayInputStream(cert.toByteArray())
            x509cert = cf.generateCertificate(input) as X509Certificate
            certPath = cf.generateCertPath(listOf(x509cert))
        }

        return certPath
    }
}
