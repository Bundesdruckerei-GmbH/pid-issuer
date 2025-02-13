/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.auth

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.COSEKey
import de.bundesdruckerei.mdoc.kotlin.core.common.hexToByteArray
import de.bundesdruckerei.mdoc.kotlin.toHexString
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DeviceKeyInfoTest {

    private val deviceKeyInfoByteString = "A3696465766963654B6579A4010220012158208553FBB8C982DB3F5A45D6FB12DFA04C0CF7A48F43657F203B3DD05BA8D62392225820334E2EDC3DDC0549F8C8DC79C10FA3BFE26B389F6AAD3E0603A6FCAE1BBFEA45716B6579417574686F72697A6174696F6E73A26A6E616D65537061636573826A6E616D655370616365316A6E616D655370616365326C64617461456C656D656E7473A26A6E616D65537061636531826A676976656E5F6E616D656B66616D696C795F6E616D656A6E616D65537061636532826B6973737565725F646174656B6578706972795F64617465676B6579496E666FA324646669766521230303"
    private lateinit var deviceKeyInfoCBOR: CBORObject

    private val deviceKeyInfoByteStringWithoutOptional = "A1696465766963654B6579A4010220012158208553FBB8C982DB3F5A45D6FB12DFA04C0CF7A48F43657F203B3DD05BA8D62392225820334E2EDC3DDC0549F8C8DC79C10FA3BFE26B389F6AAD3E0603A6FCAE1BBFEA45"
    private val deviceKeyInfoWithoutOptionalCBOR = CBORObject.DecodeFromBytes(deviceKeyInfoByteStringWithoutOptional.hexToByteArray())

    private val deviceKeyByteString = "A4010220012158208553FBB8C982DB3F5A45D6FB12DFA04C0CF7A48F43657F203B3DD05BA8D62392225820334E2EDC3DDC0549F8C8DC79C10FA3BFE26B389F6AAD3E0603A6FCAE1BBFEA45"
    private lateinit var deviceKeyCBOR: CBORObject
    private lateinit var deviceKey: COSEKey

    private val keyAuthorizationsByteString = "A26A6E616D65537061636573826A6E616D655370616365316A6E616D655370616365326C64617461456C656D656E7473A26A6E616D65537061636531826A676976656E5F6E616D656B66616D696C795F6E616D656A6E616D65537061636532826B6973737565725F646174656B6578706972795F64617465"
    private lateinit var keyAuthorizationsCBOR: CBORObject
    private lateinit var keyAuthorizations: KeyAuthorizations

    private val keyInfo: KeyInfo = mutableMapOf(Pair(-2, -4), Pair(-5, "five"), Pair(3, 3))

    private lateinit var deviceKeyInfo: DeviceKeyInfo

    @Before
    fun setUp() {
        deviceKeyInfoCBOR = CBORObject.DecodeFromBytes(deviceKeyInfoByteString.hexToByteArray())

        keyAuthorizationsCBOR = CBORObject.DecodeFromBytes(keyAuthorizationsByteString.hexToByteArray())
        keyAuthorizations = KeyAuthorizations.fromCBOR(keyAuthorizationsCBOR)

        deviceKeyCBOR = CBORObject.DecodeFromBytes(deviceKeyByteString.hexToByteArray())
        deviceKey = COSEKey(deviceKeyCBOR)

        deviceKeyInfo = DeviceKeyInfo(deviceKey, keyAuthorizations, keyInfo)

    }

    @Test
    fun asCBOR() {
        val expectedCBOR = CBORObject.DecodeFromBytes("A3696465766963654B6579A4010220012158208553FBB8C982DB3F5A45D6FB12DFA04C0CF7A48F43657F203B3DD05BA8D62392225820334E2EDC3DDC0549F8C8DC79C10FA3BFE26B389F6AAD3E0603A6FCAE1BBFEA45716B6579417574686F72697A6174696F6E73A26A6E616D65537061636573826A6E616D655370616365316A6E616D655370616365326C64617461456C656D656E7473A26A6E616D65537061636531826A676976656E5F6E616D656B66616D696C795F6E616D656A6E616D65537061636532826B6973737565725F646174656B6578706972795F64617465676B6579496E666FA22464666976652123".hexToByteArray())
        assertEquals(expectedCBOR, deviceKeyInfo.asCBOR())
    }

    @Test
    fun fromCBOR() {
        val deviceKeyInfoFromCBOR = DeviceKeyInfo.fromCBOR(deviceKeyInfoCBOR)

        assertEquals(deviceKeyInfo.deviceKey.EncodeToBytes().toHexString(), deviceKeyInfoFromCBOR.deviceKey.EncodeToBytes().toHexString())

        assertEquals(deviceKeyInfo.keyAuthorizations?.authorizedNameSpaces, deviceKeyInfoFromCBOR.keyAuthorizations?.authorizedNameSpaces)
        assertEquals(deviceKeyInfo.keyAuthorizations?.authorizedDataElements, deviceKeyInfoFromCBOR.keyAuthorizations?.authorizedDataElements)

        assertEquals(deviceKeyInfo.keyInfo?.get(0), deviceKeyInfoFromCBOR.keyInfo?.get(0))
        assertEquals(deviceKeyInfo.keyInfo?.get(1), deviceKeyInfoFromCBOR.keyInfo?.get(1))
        assertEquals(deviceKeyInfo.keyInfo?.get(2), deviceKeyInfoFromCBOR.keyInfo?.get(2))
    }

    @Test
    fun asCBORWithoutOptionalFields() {
        val deviceKeyInfoWithoutOptional = DeviceKeyInfo(deviceKey)

        assertEquals(deviceKeyInfoWithoutOptionalCBOR, deviceKeyInfoWithoutOptional.asCBOR())
    }

    @Test
    fun fromCBORWithoutOptionalFields() {
        val deviceKeyInfoWithoutOptional = DeviceKeyInfo(deviceKey)

        val deviceKeyInfoWithoutOptionalFromCBOR = DeviceKeyInfo.fromCBOR(deviceKeyInfoWithoutOptionalCBOR)

        assertEquals(deviceKeyInfoWithoutOptional.deviceKey.EncodeToBytes().toHexString(),
                deviceKeyInfoWithoutOptionalFromCBOR.deviceKey.EncodeToBytes().toHexString())

        assertEquals(deviceKeyInfoWithoutOptional.keyAuthorizations,
                deviceKeyInfoWithoutOptionalFromCBOR.keyAuthorizations)

        assertEquals(deviceKeyInfoWithoutOptional.keyInfo,
                deviceKeyInfoWithoutOptionalFromCBOR.keyInfo)
    }
}
