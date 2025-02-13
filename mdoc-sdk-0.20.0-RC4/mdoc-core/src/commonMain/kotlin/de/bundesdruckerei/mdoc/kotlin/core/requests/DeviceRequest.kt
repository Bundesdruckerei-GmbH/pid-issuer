/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.requests

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.COSEKey
import de.bundesdruckerei.mdoc.kotlin.core.DocRequests
import de.bundesdruckerei.mdoc.kotlin.core.common.DataKey.DOC_REQUESTS
import de.bundesdruckerei.mdoc.kotlin.core.common.DataKey.MAC_KEYS
import de.bundesdruckerei.mdoc.kotlin.core.common.DataKey.VERSION
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable
import de.bundesdruckerei.mdoc.kotlin.core.common.IncompatibleVersionException
import de.bundesdruckerei.mdoc.kotlin.core.common.Version
import de.bundesdruckerei.mdoc.kotlin.core.common.isApiIncompatible
import de.bundesdruckerei.mdoc.kotlin.core.common.toVersionOrNull
import de.bundesdruckerei.mdoc.kotlin.core.tstr

private const val MAJOR_VERSION = 1
private const val MINOR_VERSION = 0

class DeviceRequest(
    val version: tstr = "$MAJOR_VERSION.$MINOR_VERSION",
    val docRequests: DocRequests,
    val macKeys: Array<COSEKey>? = null
) : ICBORable {

    private val currentVersion = Version(
        major = MAJOR_VERSION,
        minor = MINOR_VERSION
    )

    init {
        val instanceVersion = version.toVersionOrNull()
        require(instanceVersion?.isValid == true) { "Invalid Version detected." }
        if (instanceVersion?.isApiIncompatible(currentVersion) == true) throw IncompatibleVersionException()
    }

    override fun asCBOR(): CBORObject {
        val macKeysCBOR = CBORObject.NewArray()
        macKeys?.forEach { macKey ->
            macKeysCBOR.Add(macKey.AsCBOR())
        }

        return CBORObject.NewMap().apply {
            Set(VERSION.key, version)
            Set(DOC_REQUESTS.key, docRequests.asCBOR())
            macKeys?.let { Set(MAC_KEYS.key, macKeysCBOR) }
        }
    }

    override fun toString(): String =
        "DeviceRequest(" +
                "version='$version', " +
                "docRequests=${docRequests.contentToString()}, " +
                "macKeys=${macKeys?.contentToString()}, " +
                "supportedVersion=$currentVersion" +
                ")"


    companion object {
        fun fromCBOR(cborObject: CBORObject): DeviceRequest {
            val version = cborObject[VERSION.key].AsString()
            val docRequestsCBOR = cborObject[DOC_REQUESTS.key]
            val macKeysCBOR = cborObject[MAC_KEYS.key] ?: null

            val docRequests = docRequestsCBOR.values.map { request ->
                DocRequest.fromCBOR(request)
            }.toTypedArray()

            val macKeys = macKeysCBOR?.values?.map { key -> COSEKey(key) }?.toTypedArray()

            return DeviceRequest(version, docRequests, macKeys)
        }

        fun fromBytes(data: ByteArray): DeviceRequest =
            fromCBOR(CBORObject.DecodeFromBytes(data))
    }
}
