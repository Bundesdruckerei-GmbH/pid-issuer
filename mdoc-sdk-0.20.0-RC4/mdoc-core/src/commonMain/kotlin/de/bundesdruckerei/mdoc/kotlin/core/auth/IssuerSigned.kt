/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.auth

import COSE.AlgorithmID
import COSE.OneKey
import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.COSEKey
import de.bundesdruckerei.mdoc.kotlin.core.DataElementIdentifier
import de.bundesdruckerei.mdoc.kotlin.core.DocType
import de.bundesdruckerei.mdoc.kotlin.core.NameSpace
import de.bundesdruckerei.mdoc.kotlin.core.NameSpacesDigests
import de.bundesdruckerei.mdoc.kotlin.core.auth.dto.DigestAlgorithm
import de.bundesdruckerei.mdoc.kotlin.core.auth.dto.ValidityRange
import de.bundesdruckerei.mdoc.kotlin.core.bstr
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable
import de.bundesdruckerei.mdoc.kotlin.core.common.log
import de.bundesdruckerei.mdoc.kotlin.core.common.toHex
import de.bundesdruckerei.mdoc.kotlin.crypto.CertUtils
import de.bundesdruckerei.mdoc.kotlin.crypto.CryptoUtils
import de.bundesdruckerei.mdoc.kotlin.crypto.DEFAULT_SECURE_RANDOM
import org.jetbrains.annotations.TestOnly
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import kotlin.random.asKotlinRandom

typealias CertificateValidationFunction = (certificates: List<X509Certificate>) -> Boolean

class IssuerSigned(val nameSpaces: IssuerNameSpaces?, val issuerAuth: IssuerAuth) : ICBORable {
    override fun asCBOR(): CBORObject {
        return CBORObject.NewMap().apply {
            if (!nameSpaces.isNullOrEmpty()) {
                Add("nameSpaces", nameSpaces.asCBOR())
            }

            Add("issuerAuth", issuerAuth.asCBOR())
        }
    }

    fun validate(
        docType: String,
        rootCertificates: Collection<X509Certificate>,
        certificateValidator: CertificateValidationFunction = { true },
        currentTimestamp: OffsetDateTime = OffsetDateTime.now()
    ): IssuerAuthValidationResult {

        val x5Chain = issuerAuth.obtainX5Chain()
        val certPath = x5Chain.toCertPath()
        val dsCert = x5Chain.endEntityCert

        val iacaCert: X509Certificate? = rootCertificates
            .firstOrNull { dsCert.issuerX500Principal.name == it.subjectX500Principal.name }

        val hasValidCertificatePath = iacaCert?.let {
            /*
               1. Validate the certificate included in the MSO header according to 9.3.3
            */
            CertUtils.validateCertificateChain(
                it,
                dsCert,
                certPath
            ) && certificateValidator(listOf(it, dsCert))
        } ?: false


        /*
            2. Verify the digital signature of the IssuerAuth structure (see 9.1.2.4) using the
            working_public_key, working_public_key_parameters, and working_public_key_algorithm
            from the certificate validation procedure of step 1.
         */
        val signerKey = OneKey(dsCert.publicKey, null)
        val hasValidSignature = issuerAuth.validate(signerKey)

        /*
            3. Calculate the digest value for every IssuerSignedItem returned in the
            DeviceResponse structure according to 9.1.2.5 and verify that these calculated digests
            equal the corresponding digest values in the MSO.
         */
        val invalidDigests = validateDigestValues(
            nameSpaces,
            issuerAuth.mso.valueDigests.digests,
            issuerAuth.mso.digestAlgorithm.value
        )
        val hasValidDigests = invalidDigests.isEmpty()

        /*
            4. Verify that the DocType in the MSO matches the relevant DocType in the Documents structure.
         */
        val msoDocType = issuerAuth.mso.docType
        val hasValidDoctype = docType.equals(msoDocType, true)

        /*
            5. Validate the elements in the ValidityInfo structure
         */
        val hasValidValidityInfo = validateValidityInfo(
            issuerAuth.mso.validityInfo,
            dsCert,
            currentTimestamp
        )

        return IssuerAuthValidationResult(
            hasValidCertificatePath = hasValidCertificatePath,
            hasValidSignature = hasValidSignature,
            hasValidDigests = hasValidDigests,
            invalidDigests = invalidDigests,
            hasValidDocType = hasValidDoctype,
            hasValidValidityInfo = hasValidValidityInfo
        )
    }

    /**
     * Method for validating that calculated digests are equal the corresponding digest values in the MSO
     *
     * @param nameSpaces data which is used for calculating digests
     * @param msoDigests that need to match IssuerNameSpaces digests
     * @param digestAlgorithm which is used for hashing
     * @return DigestValidationResult object
     */
    private fun validateDigestValues(
        nameSpaces: IssuerNameSpaces?,
        msoDigests: NameSpacesDigests,
        digestAlgorithm: String
    ): Map<NameSpace, List<Pair<IssuerSignedItem, bstr?>>> {
        val invalidDigests = mutableMapOf<NameSpace, List<Pair<IssuerSignedItem, bstr?>>>()

        nameSpaces?.forEach { (nameSpace, issuerSignedItems) ->
            val invalidDigestsInNamespace = mutableListOf<Pair<IssuerSignedItem, bstr?>>()
            issuerSignedItems.forEach { issuerSignedItem ->
                val digestID = issuerSignedItem.digestID
                val digest = msoDigests[nameSpace]?.get(digestID)

                val input = issuerSignedItem.asTaggedCBORBytes().EncodeToBytes()
                var calculatedDigest: ByteArray? = null

                try {
                    calculatedDigest = input?.let {
                        CryptoUtils.digest(it, digestAlgorithm)
                    }
                } catch (e: Exception) {
                    e.message?.let { log.d(it) }
                }

                if (!calculatedDigest.contentEquals(digest)) {
                    log.e(
                        "IssuerItem ${
                            issuerSignedItem.asTaggedCBORBytes().EncodeToBytes().toHex()
                        }"
                    )
                    log.e("Digest ${digest?.toHex()}")
                    log.e("Digest with digestID $digestID within $nameSpace nameSpace is not valid!")
                    invalidDigestsInNamespace.add(Pair(issuerSignedItem, digest))
                }
            }
            if (invalidDigestsInNamespace.isNotEmpty()) {
                invalidDigests[nameSpace] = invalidDigestsInNamespace
            }
        }
        return invalidDigests
    }

    /**
     * Method for validating ValidityInfo structure and checking if the dates
     * are valid and within the proper time period
     *
     * @param validityInfo ValidityInfo structure with dates
     * @param dsCert Target X509 Certificate
     * @return ValidityInfoValidationResult object
     */
    private fun validateValidityInfo(
        validityInfo: ValidityInfo,
        dsCert: X509Certificate?,
        currentTimestamp: OffsetDateTime = OffsetDateTime.now()

    ): Boolean {
        val signedDateIsValid =
            checkIfSignedDateIsWithinCertificatesValidityPeriod(validityInfo, dsCert)

        val validFromIsValid =
            checkIfValidFromDateIsBeforeCurrentTimeStamp(validityInfo, currentTimestamp)
        val validUntilIsValid =
            checkIfValidUntilDateIsAfterCurrentTimeStamp(validityInfo, currentTimestamp)

        return signedDateIsValid && validFromIsValid && validUntilIsValid
    }

    private fun checkIfSignedDateIsWithinCertificatesValidityPeriod(
        validityInfo: ValidityInfo,
        dsCert: X509Certificate?
    ): Boolean {
        var result = true

        try {
            dsCert?.checkValidity(validityInfo.signed)
        } catch (dtpe: DateTimeParseException) {
            log.e("Certificate's signed date is not valid!", dtpe)
            result = false
        } catch (e: Exception) {
            log.e("Signed date is not within the certificate's validity period!", e)
            result = false
        }

        return result
    }

    private fun checkIfValidUntilDateIsAfterCurrentTimeStamp(
        validityInfo: ValidityInfo,
        currentTimestamp: OffsetDateTime
    ): Boolean {
        var result = true

        try {
            val validUntilDate: OffsetDateTime =
                OffsetDateTime.ofInstant(
                    validityInfo.validUntil.toInstant(),
                    currentTimestamp.offset
                )
            val isAfter = validUntilDate.isAfter(currentTimestamp)

            if (!isAfter) {
                log.e("ValidUntil date is not after the current timestamp!")
                result = false
            }
        } catch (dtpe: DateTimeParseException) {
            log.e("Certificate's valid until date is not valid!")
            log.e(dtpe.message.toString())
            result = false
        }

        return result
    }

    private fun checkIfValidFromDateIsBeforeCurrentTimeStamp(
        validityInfo: ValidityInfo,
        currentTimestamp: OffsetDateTime
    ): Boolean {
        var result = true

        try {
            val validFromDate: OffsetDateTime =
                OffsetDateTime.ofInstant(
                    validityInfo.validFrom.toInstant(),
                    currentTimestamp.offset
                )
            val isBefore = validFromDate.isBefore(currentTimestamp)

            if (!isBefore) {
                log.e("ValidFrom date is not before the current timestamp!")
                result = false
            }
        } catch (dtpe: DateTimeParseException) {
            log.e("Certificate's valid from date is not valid!")
            log.e(dtpe.message.toString())
            result = false
        }

        return result
    }

    /**
     * A convenience Builder class which can be used to create [IssuerSigned] instances from plain data.
     *
     * This Builder provides various functions to specify the issued name space items as well as necessary meta
     * information like validity or key information and parameters for algorithms or digest generation.
     *
     * This Builder will default everything that has not explicitly been configured. Consult the documentation of the
     * respective getters for the used defaults.
     *
     * @constructor Creates a new [IssuerSigned.Builder] from the given [docType] and [certificates].
     * @param docType The [DocType] of the [Document][de.bundesdruckerei.mdoc.kotlin.core.Document] to hold the
     * generated [IssuerSigned] instance. This will be added to the [IssuerAuth.mso]
     * @param certificates the [X5Chain] to be added to the generated [IssuerAuth] instance.
     * the [endEntityCert][X5Chain.endEntityCert] must be the one to verify the signature with after signing.
     * @param status The status claim for the [MobileSecurityObject] to provide revocation information.
     */
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    class Builder(val docType: DocType, val certificates: X5Chain, val status: StatusClaim? = null) {

        private val _nameSpaces: MutableMap<NameSpace, MutableMap<DataElementIdentifier, CBORObject>> =
            mutableMapOf()

        private var valueDigests: ValueDigests? = null

        /**
         * The currently configured name space data items. Default is an empty map. Use the put functions to populate.
         */
        val nameSpaces: Map<NameSpace, Map<DataElementIdentifier, CBORObject>> = _nameSpaces

        /**
         * The [ValidityRange] used create the [ValidityInfo] for the the [IssuerAuth.mso]. Defaults to the validity
         * of the [endEntityCert][X5Chain.endEntityCert] in the given [certificates] with an
         * [expectedUpdate][ValidityRange.expectedUpdate] of `null`.
         */
        @set:JvmSynthetic
        @set:JvmName("kotlinSetValidityRange")
        var validityRange = ValidityRange(
            validFrom = certificates.endEntityCert.notBefore.toInstant(),
            validUntil = certificates.endEntityCert.notAfter.toInstant()
        )

        /**
         * The [DeviceKeyInfo] to be included to the [IssuerAuth.mso].
         */
        @set:JvmSynthetic
        @set:JvmName("kotlinSetDeviceKeyInfo")
        var deviceKeyInfo: DeviceKeyInfo? = null

        /**
         * The algorithm used for signing [IssuerAuth] instance. Defaults to [AlgorithmID.ECDSA_256].
         */
        @set:JvmSynthetic
        @set:JvmName("kotlinSetSigningAlgorithm")
        var signingAlgorithm = AlgorithmID.ECDSA_256

        /**
         * The algorithm used for calculating the [ValueDigests]. Defaults to [DigestAlgorithm.SHA_256].
         */
        @set:JvmSynthetic
        @set:JvmName("kotlinSetDigestAlgorithm")
        var digestAlgorithm = DigestAlgorithm.SHA_256

        @set:TestOnly
        @get:TestOnly
        @set:JvmSynthetic
        @set:JvmName("kotlinSetNowOverride")
        internal var nowOverride: Instant? = null

        /**
         * The amount of random bytes to generate for each [IssuerSignedItem.random].
         * Defaults to [MIN_RANDOM_BYTES_LENGTH]. Attempts to set this property to a value lower than
         * [MIN_RANDOM_BYTES_LENGTH] will set it to [MIN_RANDOM_BYTES_LENGTH] instead.
         */
        @set:JvmSynthetic
        @set:JvmName("kotlinSetRandomBytesLength")
        var randomBytesLength = MIN_RANDOM_BYTES_LENGTH
            set(value) {
                field = value.coerceAtLeast(MIN_RANDOM_BYTES_LENGTH)
            }

        /**
         * The [SecureRandom] instance to use for generating each [IssuerSignedItem.random].
         * Defaults to [DEFAULT_SECURE_RANDOM]
         */
        @set:JvmSynthetic
        @set:JvmName("kotlinSetRandom")
        var random: SecureRandom = DEFAULT_SECURE_RANDOM

        /**
         * Adds the given [value] to the specified [nameSpace] using the given [identifier].
         *
         * If the specified [nameSpace] does not exist, it will be created.
         *
         * If the specified [nameSpace] already contains an item with the same [identifier], it will be replaced.
         *
         * @return this Builder.
         */
        fun putItem(
            nameSpace: NameSpace,
            identifier: DataElementIdentifier,
            value: CBORObject
        ): Builder {
            _nameSpaces.getOrPut(nameSpace, ::mutableMapOf)[identifier] = value
            return this
        }

        /**
         * Adds the given [items] to the specified [nameSpace] using.
         *
         * If the specified [nameSpace] does not exist, it will be created.
         *
         * If an item exists for the same identifier, it will be replaced.
         *
         * @return this Builder.
         */
        fun putItems(nameSpace: NameSpace, items: Map<DataElementIdentifier, CBORObject>): Builder {
            _nameSpaces[nameSpace]
                ?.putAll(items)
                ?: _nameSpaces.put(nameSpace, items.toMutableMap())
            return this
        }

        /**
         * Adds the all values from the given [nameSpaces] to this Builder's [nameSpaces][Builder.nameSpaces].
         *
         * If a given name space already exists, the given name space is merged into the existing one.
         *
         * If an item exists for the same combination of name space and identifier, it will be replaced.
         *
         * @return this Builder.
         */
        fun putItems(nameSpaces: Map<NameSpace, Map<DataElementIdentifier, CBORObject>>): Builder {
            nameSpaces.forEach(::putItems)
            return this
        }

        /**
         * Sets the specified [nameSpace] to the given [items].
         *
         * If the specified [nameSpace] already exists, it's items will be replaced entirely with the new ones.
         *
         * @return this Builder.
         */
        fun putNameSpace(
            nameSpace: NameSpace,
            items: Map<DataElementIdentifier, CBORObject>
        ): Builder {
            _nameSpaces[nameSpace] = items.toMutableMap()
            return this
        }

        /**
         * Sets the specified [nameSpaces].
         *
         * If a name space already exists, it's items will be replaced entirely with the new ones.
         *
         * @return this Builder.
         */
        fun putNameSpaces(nameSpaces: Map<NameSpace, Map<DataElementIdentifier, CBORObject>>): Builder {
            nameSpaces.forEach(::putNameSpace)
            return this
        }

        /**
         * Removes a single item from the specified [nameSpace] using the given [identifier].
         *
         * If the specified item is the only item under the specified [nameSpace], removes the whole [nameSpace].
         *
         * @return this Builder.
         */
        fun removeItem(nameSpace: NameSpace, identifier: DataElementIdentifier): Builder {
            _nameSpaces[nameSpace]?.run {
                remove(identifier)
                if (isEmpty()) _nameSpaces.remove(nameSpace)
            }
            return this
        }

        /**
         * Removes all items matching the given [identifiers] from the specified [nameSpace].
         *
         * If the specified [nameSpace] should be empty after this operation, removes the [nameSpace] as well.
         *
         * @return this Builder.
         */
        fun removeItems(
            nameSpace: NameSpace,
            identifiers: Collection<DataElementIdentifier>
        ): Builder {
            _nameSpaces[nameSpace]?.run {
                identifiers.forEach(::remove)
                if (isEmpty()) _nameSpaces.remove(nameSpace)
            }
            return this
        }

        /**
         * Removes all items matching the given [identifiers] from the specified [nameSpace].
         *
         * If the specified [nameSpace] should be empty after this operation, removes the [nameSpace] as well.
         *
         * @return this Builder.
         */
        fun removeItems(nameSpace: NameSpace, vararg identifiers: DataElementIdentifier): Builder {
            _nameSpaces[nameSpace]?.run {
                identifiers.forEach(::remove)
                if (isEmpty()) _nameSpaces.remove(nameSpace)
            }
            return this
        }

        /**
         * Removes the specified [nameSpace] from this Builder.
         *
         * @return this Builder.
         */
        fun removeNameSpace(nameSpace: NameSpace): Builder {
            _nameSpaces.remove(nameSpace)
            return this
        }

        /**
         * Removes the specified [nameSpaces] from this Builder.
         *
         * @return this Builder.
         */
        fun removeNameSpaces(nameSpaces: Collection<NameSpace>): Builder {
            nameSpaces.forEach(_nameSpaces::remove)
            return this
        }

        /**
         * Removes the specified [nameSpaces] from this Builder.
         *
         * @return this Builder.
         */
        fun removeNameSpaces(vararg nameSpaces: NameSpace): Builder {
            nameSpaces.forEach(_nameSpaces::remove)
            return this
        }

        /**
         * Removes all name spaces from this Builder.
         *
         * @return this Builder.
         */
        fun clearNameSpaces(): Builder {
            _nameSpaces.clear()
            return this
        }

        /**
         * Sets this Builder's [validityRange][Builder.validityRange] to the given [validityRange].
         *
         * @return this Builder.
         */
        fun setValidityRange(validityRange: ValidityRange): Builder {
            this.validityRange = validityRange
            return this
        }

        /**
         * Sets the Builder's [deviceKeyInfo] to the given [deviceKeyInfo].
         *
         * @return this Builder.
         */
        fun setDeviceKeyInfo(deviceKeyInfo: DeviceKeyInfo): Builder {
            this.deviceKeyInfo = deviceKeyInfo
            return this
        }

        /**
         * Sets this Builder's [signingAlgorithm] to the given [algorithmID].
         *
         * @return this Builder.
         */
        fun setSigningAlgorithm(algorithmID: AlgorithmID): Builder {
            this.signingAlgorithm = algorithmID
            return this
        }

        /**
         * Sets this Builder's [digestAlgorithm][Builder.digestAlgorithm] to the given [digestAlgorithm].
         *
         * @return this Builder.
         */
        fun setDigestAlgorithm(digestAlgorithm: DigestAlgorithm): Builder {
            this.digestAlgorithm = digestAlgorithm
            return this
        }

        /**
         * Sets this Builder's [randomBytesLength] to the given [newLength].
         *
         * If [newLength] is lower than [MIN_RANDOM_BYTES_LENGTH], sets [randomBytesLength] to
         * [MIN_RANDOM_BYTES_LENGTH].
         *
         * @return this Builder.
         */
        fun setRandomBytesLength(newLength: Int): Builder {
            this.randomBytesLength = newLength
            return this
        }

        /**
         * Sets this Builder's [random][Builder.random] to the given [random].
         *
         * @return this Builder.
         */
        fun setRandom(random: SecureRandom): Builder {
            this.random = random
            return this
        }

        /**
         * Disables auto generation of value digests and sets the given [valueDigests].
         *
         * @return this Builder.
         */
        fun overrideValueDigests(valueDigests: ValueDigests): Builder {
            this.valueDigests = valueDigests
            return this
        }

        @TestOnly
        internal fun setNowOverride(nowOverride: Instant?): Builder {
            this.nowOverride = nowOverride
            return this
        }

        /**
         * Creates a new instance of [IssuerSigned] from this Builder's current state.
         *
         * The included [IssuerAuth] will have the necessary headers and external data set for signing.
         *
         * This method can be called multiple times. Each time will create a new [IssuerSigned] instance with
         * different random bytes and digests.
         *
         * @return An unsigned instance of [IssuerSigned].
         * @throws IllegalArgumentException if [validityRange] has been exceeded by the time of creating the
         * @throws IllegalArgumentException if [deviceKeyInfo] has not been set
         * [IssuerSigned] instance.
         */
        fun build(): IssuerSigned {
            val kRandom = random.asKotlinRandom()

            val localDeviceKeyInfo =
                requireNotNull(deviceKeyInfo) { "deviceKeyInfo must not be null" }

            val nameSpaces = nameSpaces.mapValues { (_, items) ->
                items.entries
                    .shuffled(kRandom)
                    .mapIndexed { digestID, (elementIdentifier, elementValue) ->
                        IssuerSignedItem(
                            elementIdentifier = elementIdentifier,
                            elementValue = elementValue,
                            digestID = digestID.toLong(),
                            random = kRandom.nextBytes(randomBytesLength)
                        )
                    }
            }

            return IssuerSigned(
                nameSpaces = nameSpaces,
                issuerAuth = when (val nowOverride = this.nowOverride) {
                    null -> IssuerAuth.create(
                        docType = docType,
                        nameSpaces = nameSpaces,
                        valueDigests = valueDigests,
                        dsCertificates = certificates,
                        deviceKeyInfo = localDeviceKeyInfo,
                        validityRange = validityRange,
                        signatureAlgorithm = signingAlgorithm,
                        digestAlgorithm = digestAlgorithm,
                        status = status
                    )

                    else -> IssuerAuth.create(
                        docType = docType,
                        nameSpaces = nameSpaces,
                        valueDigests = valueDigests,
                        dsCertificates = certificates,
                        deviceKeyInfo = localDeviceKeyInfo,
                        validityRange = validityRange,
                        signatureAlgorithm = signingAlgorithm,
                        digestAlgorithm = digestAlgorithm,
                        getNow = { nowOverride },
                        status = status
                    )
                }
            )
        }

        /**
         * Creates a new instance of [IssuerSigned] from this Builder's current state and it's [IssuerAuth] with the
         * given [key].
         *
         * This method can be called multiple times. Each time will create a new [IssuerSigned] instance with
         * different random bytes and digests.
         *
         * @param key the [COSEKey] to sign the [IssuerAuth] with. Must contain a [PrivateKey].
         *
         * @return A signed instance of [IssuerSigned].
         * @throws IllegalArgumentException if [validityRange] has been exceeded by the time of creating the
         * [IssuerSigned] instance.
         * @throws COSE.CoseException thrown by [IssuerAuth.sign].
         */
        fun buildAndSign(key: COSEKey) = build().apply { issuerAuth.sign(key) }

        /**
         * Creates a new instance of [IssuerSigned] from this Builder's current state and it's [IssuerAuth] with the
         * given [key].
         *
         * This method can be called multiple times. Each time will create a new [IssuerSigned] instance with
         * different random bytes and digests.
         *
         * @param key the [PrivateKey] to sign the [IssuerAuth] with.
         *
         * @return A signed instance of [IssuerSigned].
         * @throws IllegalArgumentException if [validityRange] has been exceeded by the time of creating the
         * [IssuerSigned] instance.
         * @throws COSE.CoseException thrown by [IssuerAuth.sign].
         */
        fun buildAndSign(key: PrivateKey) =
            buildAndSign(COSEKey(certificates.endEntityCert.publicKey, key))
    }

    companion object {

        // As defined in ISO/IEC 18013-5:2021 section 9.1.2.5
        const val MIN_RANDOM_BYTES_LENGTH = 16

        fun fromCBOR(cborObject: CBORObject): IssuerSigned {
            val nameSpaces: MutableMap<NameSpace, IssuerSignedItems> = mutableMapOf()
            val issuerNameSpaces = cborObject["nameSpaces"]

            for ((nameSpace, isItems) in issuerNameSpaces.entries) {
                val isItemsList = mutableListOf<IssuerSignedItem>()

                isItems.values.forEach {
                    isItemsList.add(IssuerSignedItem.fromTaggedCBOR(it))
                }

                nameSpaces[nameSpace.AsString()] = isItemsList
            }

            val issuerAuth = IssuerAuth.fromCBOR(cborObject["issuerAuth"])

            return IssuerSigned(nameSpaces, issuerAuth)
        }

        /**
         * Returns a new [IssuerSigned] instance created by [Builder] using [Builder.build].
         *
         * @param docType see [Builder.docType].
         * @param certificates see [Builder.certificates].
         * @param block DSL block to configure the [Builder].
         *
         * @throws IllegalArgumentException if [Builder.validityRange] has been exceeded by the time of creating the
         * [IssuerSigned] instance.
         */
        @Suppress("unused")
        @JvmSynthetic
        inline fun build(
            docType: DocType,
            certificates: X5Chain,
            block: Builder.() -> Unit
        ): IssuerSigned = Builder(docType, certificates)
            .apply(block)
            .build()

        /**
         * Returns a new [IssuerSigned] instance created by [Builder] using [Builder.buildAndSign].
         *
         * @param docType see [Builder.docType].
         * @param certificates see [Builder.certificates].
         * @param signingKey the [COSEKey] to sign the [IssuerAuth] with. Must contain a [PrivateKey].
         * @param block DSL block to configure the [Builder].
         *
         * @throws IllegalArgumentException if [Builder.validityRange] has been exceeded by the time of creating the
         * [IssuerSigned] instance.
         * @throws COSE.CoseException thrown by [IssuerAuth.sign].
         */
        @Suppress("unused")
        @JvmSynthetic
        inline fun buildAndSign(
            docType: DocType,
            certificates: X5Chain,
            signingKey: COSEKey,
            block: Builder.() -> Unit
        ): IssuerSigned = Builder(docType, certificates)
            .apply(block)
            .buildAndSign(signingKey)

        /**
         * Returns a new [IssuerSigned] instance created by [Builder] using [Builder.buildAndSign].
         *
         * @param docType see [Builder.docType].
         * @param certificates see [Builder.certificates].
         * @param signingKey the [PrivateKey] to sign the [IssuerAuth] with.
         * @param block DSL block to configure the [Builder].
         *
         * @throws IllegalArgumentException if [Builder.validityRange] has been exceeded by the time of creating the
         * [IssuerSigned] instance.
         * @throws COSE.CoseException thrown by [IssuerAuth.sign].
         */
        @Suppress("unused")
        @JvmSynthetic
        inline fun buildAndSign(
            docType: DocType,
            certificates: X5Chain,
            signingKey: PrivateKey,
            block: Builder.() -> Unit
        ): IssuerSigned = Builder(docType, certificates)
            .apply(block)
            .buildAndSign(signingKey)
    }
}
