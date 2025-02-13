/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.deviceauth

import COSE.OneKey
import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBORTypeMapper
import com.upokecenter.cbor.ICBORConverter
import de.bundesdruckerei.mdoc.kotlin.core.COSEKey
import de.bundesdruckerei.mdoc.kotlin.core.DataElementIdentifier
import de.bundesdruckerei.mdoc.kotlin.core.DocType
import de.bundesdruckerei.mdoc.kotlin.core.NameSpace
import de.bundesdruckerei.mdoc.kotlin.core.SessionTranscript
import de.bundesdruckerei.mdoc.kotlin.core.auth.DeviceKeyInfo
import de.bundesdruckerei.mdoc.kotlin.core.auth.KeyAuthorizations
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable
import de.bundesdruckerei.mdoc.kotlin.core.common.log
import de.bundesdruckerei.mdoc.kotlin.core.deviceauth.ContextTag.Mac
import de.bundesdruckerei.mdoc.kotlin.core.deviceauth.ContextTag.Sig
import org.bouncycastle.crypto.EphemeralKeyPair
import java.security.Signature

data class DeviceSigned(
    val nameSpaces: DeviceNameSpaces,
    val deviceAuth: DeviceAuth
) : ICBORable {
    override fun asCBOR(): CBORObject = CBORObject.FromObject(this, cborMapper)

    fun validate(
        docType: DocType,
        deviceKeyInfo: DeviceKeyInfo,
        readerMacKey: EphemeralKeyPair?,
        sessionTranscript: SessionTranscript,
    ): DeviceAuthValidationResult {

        val devicePublicKey = OneKey(
            deviceKeyInfo.deviceKey.AsPublicKey(),
            null
        )

        val unauthorizedKeyUsage = validateNameSpaceAuthorizations(
            deviceNameSpaces = nameSpaces,
            keyAuthorizations = deviceKeyInfo.keyAuthorizations
        )

        val taggedDeviceAuth = DeviceAuthentication(
            sessionTranscript,
            docType,
            CBORObject.DecodeFromBytes(nameSpaces.asTaggedCBOR().EncodeToBytes())
        )
        try {
            deviceAuth.asCBOR().keys.forEach {
                return when (it.AsString()) {
                    Mac.tag -> DeviceAuthValidationResult(
                        unauthorizedKeyUsage = unauthorizedKeyUsage,
                        hasValidMac = validateForDeviceMac(
                            readerMacKey,
                            sessionTranscript,
                            devicePublicKey,
                            taggedDeviceAuth
                        )
                    )

                    Sig.tag -> DeviceAuthValidationResult(
                        unauthorizedKeyUsage = unauthorizedKeyUsage,
                        hasValidSignature = validateForDeviceSignature(
                            taggedDeviceAuth,
                            devicePublicKey
                        )
                    )

                    else -> DeviceAuthValidationResult(unauthorizedKeyUsage = unauthorizedKeyUsage)
                }
            }
        } catch (ex: Exception) {
            log.e("${ex.message}, $ex")
        }

        return DeviceAuthValidationResult(unauthorizedKeyUsage = unauthorizedKeyUsage)
    }

    private fun validateForDeviceMac(
        readerMacKey: EphemeralKeyPair?,
        sessionTranscript: SessionTranscript,
        devicePublicKey: OneKey,
        deviceAuthentication: DeviceAuthentication
    ): Boolean = if (readerMacKey == null) {
        log.e("no reader key given")
        false
    } else {
        val macKey = DeviceMac.calculateMacKey(
            sessionTranscript,
            devicePublicKey,
            readerMacKey.keyPair.private
        )
        val deviceMac = deviceAuth.devAuth as DeviceMac
        deviceMac.isValid(deviceAuthentication, macKey)
    }

    private fun validateForDeviceSignature(
        deviceAuthentication: DeviceAuthentication,
        devicePublicKey: OneKey
    ): Boolean {
        val deviceSignature = this.deviceAuth.devAuth as DeviceSignature
        return deviceSignature.isValid(deviceAuthentication, devicePublicKey)
    }

    companion object {
        @Suppress("ObjectLiteralToLambda")
        val cborConverter = object : ICBORConverter<DeviceSigned> {
            override fun ToCBORObject(obj: DeviceSigned): CBORObject {
                return CBORObject.NewMap()
                    .Set("nameSpaces", obj.nameSpaces.asTaggedCBOR())
                    .Set("deviceAuth", obj.deviceAuth.asCBOR())
            }
        }

        private val cborMapper: CBORTypeMapper =
            CBORTypeMapper().AddConverter(DeviceSigned::class.java, cborConverter)

        fun fromCBOR(cborObject: CBORObject): DeviceSigned {
            val nameSpaces = DeviceNameSpaces.fromTaggedCBOR(cborObject["nameSpaces"])
            val deviceAuth = DeviceAuth.fromCBOR(cborObject["deviceAuth"])
            return DeviceSigned(nameSpaces, deviceAuth)
        }

        fun from(
            sharedSecret: ByteArray,
            sessionTranscript: SessionTranscript,
            docType: DocType,
            nameSpaces: DeviceNameSpaces,
            curveName: String,
        ): Result<DeviceSigned> = runCatching {
            val deviceAuthentication = DeviceAuthentication(
                sessionTranscript = sessionTranscript,
                docType = docType,
                deviceNameSpacesBytes = nameSpaces.asTaggedCBOR()
            )

            val deviceMac = DeviceMac.from(
                deviceAuthentication = deviceAuthentication,
                curveName = curveName,
                sharedSecret = sharedSecret,
                sessionTranscriptBytes = sessionTranscript.asBytes()
            )
            DeviceSigned(nameSpaces, DeviceAuth(deviceMac))
        }

        fun from(
            devicePrivateKey: COSEKey,
            deviceAuthentication: DeviceAuthentication,
            nameSpaces: DeviceNameSpaces,
            curveName: String
        ): Result<DeviceSigned> = runCatching {
            val deviceSignature = DeviceSignature(
                deviceAuthentication = deviceAuthentication,
                authenticationKey = devicePrivateKey,
                curveName = curveName
            )
            DeviceSigned(
                nameSpaces = nameSpaces,
                deviceAuth = DeviceAuth(deviceSignature)
            )
        }

        fun from(
            signature: Signature,
            deviceAuthentication: DeviceAuthentication,
            nameSpaces: DeviceNameSpaces
        ): Result<DeviceSigned> = runCatching {
            val deviceSignature = DeviceSignature.from(
                signature = signature,
                deviceAuthentication = deviceAuthentication
            )
            val deviceAuth = DeviceAuth(deviceSignature)
            DeviceSigned(
                nameSpaces = nameSpaces,
                deviceAuth = deviceAuth
            )
        }

        fun validateNameSpaceAuthorizations(
            deviceNameSpaces: DeviceNameSpaces,
            keyAuthorizations: KeyAuthorizations?
        ): Map<NameSpace, List<DataElementIdentifier>> {

            if (keyAuthorizations == null) return emptyMap()

            val missingNameSpaceAuthorizations =
                mutableMapOf<NameSpace, List<DataElementIdentifier>>()

            deviceNameSpaces.value.forEach { (nameSpace, deviceSignedItems) ->

                if (keyAuthorizations.authorizedNameSpaces?.contains(nameSpace) != true) {
                    val missingDataElementsAuthorizations = mutableListOf<DataElementIdentifier>()
                    val authorizedDataElements =
                        keyAuthorizations.authorizedDataElements?.get(nameSpace)
                    deviceSignedItems.items.forEach { deviceSignedItem ->
                        if (authorizedDataElements?.contains(deviceSignedItem.first) != true) {
                            missingDataElementsAuthorizations.add(deviceSignedItem.first)
                        }
                    }
                    if (missingDataElementsAuthorizations.isNotEmpty()) {
                        missingNameSpaceAuthorizations[nameSpace] =
                            missingDataElementsAuthorizations
                    }
                }
            }

            return missingNameSpaceAuthorizations
        }
    }
}
