/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.auth

import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBORObject.DecodeFromBytes
import de.bundesdruckerei.mdoc.kotlin.core.common.hexToByteArray
import de.bundesdruckerei.mdoc.kotlin.toHexString
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.OffsetDateTime
import java.util.Date

class MobileSecurityObjectTest {
    /*
        {
            "version": "1.0",
            "digestAlgorithm": "SHA-256",
            "valueDigests": {
                "org.iso.18013.5.1": {
                    1: h'af307ad59c28c5032c1a49394e0bce7cb673a533d9ad12bd4ca980f6f12db5be',
                    2: h'76aac7b52f938a4aeae31d796ba5aeb2dbe0c9d96ad75b18d75afae875a79c76',
                    24_0: h'580bae0ecf9a1c4d162586d7520615e2a7ac14584967fc9bbe8373e59302ca98',
                },
                "org.iso.18013.5.1.Utopia": {
                    52_0: h'8d5fff80e966d52c123c34b07bd6546909ad7ae6bf48d736e0865a8b99853b7f',
                },
            },
            "deviceKey": {
                1: 2,
                -1: 1,
                -2: h'8553fbb8c982db3f5a45d6fb12dfa04c0cf7a48f43657f203b3dd05ba8d62392',
                -3: h'334e2edc3ddc0549f8c8dc79c10fa3bfe26b389f6aad3e0603a6fcae1bbfea45',
            },
            "deviceKeyInfo": {
                "deviceKey": {
                    1: 2,
                    -1: 1,
                    -2: h'8553fbb8c982db3f5a45d6fb12dfa04c0cf7a48f43657f203b3dd05ba8d62392',
                    -3: h'334e2edc3ddc0549f8c8dc79c10fa3bfe26b389f6aad3e0603a6fcae1bbfea45',
                },
                "keyAuthorizations": {"nameSpaces": ["org.iso.18013.5.1"]},
                "keyInfo": {-5: "five", -2: -4},
            },
            "docType": "org.iso.18013.5.1.mDL",
            "validityInfo": {
                "signed": 0("2019-08-08T00:00:00Z"),
                "validFrom": 0("2019-08-24T00:00:00Z"),
                "validUntil": 0("2029-02-28T00:00:00Z"),
            },
        }
     */
    private val mobileSecurityObjectByteString =
        "A76776657273696F6E63312E306F646967657374416C676F726974686D675348412D3235366C76616C756544696765737473A2716F72672E69736F2E31383031332E352E31A3015820AF307AD59C28C5032C1A49394E0BCE7CB673A533D9AD12BD4CA980F6F12DB5BE02582076AAC7B52F938A4AEAE31D796BA5AEB2DBE0C9D96AD75B18D75AFAE875A79C7618185820580BAE0ECF9A1C4D162586D7520615E2A7AC14584967FC9BBE8373E59302CA9878186F72672E69736F2E31383031332E352E312E55746F706961A1183458208D5FFF80E966D52C123C34B07BD6546909AD7AE6BF48D736E0865A8B99853B7F696465766963654B6579A4010220012158208553FBB8C982DB3F5A45D6FB12DFA04C0CF7A48F43657F203B3DD05BA8D62392225820334E2EDC3DDC0549F8C8DC79C10FA3BFE26B389F6AAD3E0603A6FCAE1BBFEA456D6465766963654B6579496E666FA3696465766963654B6579A4010220012158208553FBB8C982DB3F5A45D6FB12DFA04C0CF7A48F43657F203B3DD05BA8D62392225820334E2EDC3DDC0549F8C8DC79C10FA3BFE26B389F6AAD3E0603A6FCAE1BBFEA45716B6579417574686F72697A6174696F6E73A16A6E616D6553706163657381716F72672E69736F2E31383031332E352E31676B6579496E666FA2246466697665212367646F6354797065756F72672E69736F2E31383031332E352E312E6D444C6C76616C6964697479496E666FA3667369676E6564C074323031392D30382D30385430303A30303A30305A6976616C696446726F6DC074323031392D30382D32345430303A30303A30305A6A76616C6964556E74696CC074323032392D30322D32385430303A30303A30305A"

    /*
    {
        "status": {
            "status_list": {"idx": 412_1, "uri": "https://example.com/statuslists/1"},
        },
        "docType": "org.iso.18013.5.1.mDL",
        "version": "1.0",
        "validityInfo": {
            "signed": 0("2019-08-08T00:00:00Z"),
            "validFrom": 0("2019-08-24T00:00:00Z"),
            "validUntil": 0("2029-02-28T00:00:00Z"),
        },
        "valueDigests": {
            "org.iso.18013.5.1": {
                1: h'af307ad59c28c5032c1a49394e0bce7cb673a533d9ad12bd4ca980f6f12db5be',
                2: h'76aac7b52f938a4aeae31d796ba5aeb2dbe0c9d96ad75b18d75afae875a79c76',
                24_0: h'580bae0ecf9a1c4d162586d7520615e2a7ac14584967fc9bbe8373e59302ca98',
            },
            "org.iso.18013.5.1.Utopia": {
                52_0: h'8d5fff80e966d52c123c34b07bd6546909ad7ae6bf48d736e0865a8b99853b7f',
            },
        },
        "deviceKeyInfo": {
            "keyInfo": {-2: -4, -5: "five"},
            "deviceKey": {
                1: 2,
                -1: 1,
                -2: h'8553fbb8c982db3f5a45d6fb12dfa04c0cf7a48f43657f203b3dd05ba8d62392',
                -3: h'334e2edc3ddc0549f8c8dc79c10fa3bfe26b389f6aad3e0603a6fcae1bbfea45',
            },
            "keyAuthorizations": {"nameSpaces": ["org.iso.18013.5.1"]},
        },
        "digestAlgorithm": "SHA-256",
    }
     */
    private val mobileSecurityObjectByteStringWithStatus =
        "A766737461747573A16B7374617475735F6C697374A26369647819019C63757269782168747470733A2F2F6578616D706C652E636F6D2F7374617475736C697374732F3167646F6354797065756F72672E69736F2E31383031332E352E312E6D444C6776657273696F6E63312E306C76616C6964697479496E666FA3667369676E6564C074323031392D30382D30385430303A30303A30305A6976616C696446726F6DC074323031392D30382D32345430303A30303A30305A6A76616C6964556E74696CC074323032392D30322D32385430303A30303A30305A6C76616C756544696765737473A2716F72672E69736F2E31383031332E352E31A3015820AF307AD59C28C5032C1A49394E0BCE7CB673A533D9AD12BD4CA980F6F12DB5BE02582076AAC7B52F938A4AEAE31D796BA5AEB2DBE0C9D96AD75B18D75AFAE875A79C7618185820580BAE0ECF9A1C4D162586D7520615E2A7AC14584967FC9BBE8373E59302CA9878186F72672E69736F2E31383031332E352E312E55746F706961A1183458208D5FFF80E966D52C123C34B07BD6546909AD7AE6BF48D736E0865A8B99853B7F6D6465766963654B6579496E666FA3676B6579496E666FA22123246466697665696465766963654B6579A4010220012158208553FBB8C982DB3F5A45D6FB12DFA04C0CF7A48F43657F203B3DD05BA8D62392225820334E2EDC3DDC0549F8C8DC79C10FA3BFE26B389F6AAD3E0603A6FCAE1BBFEA45716B6579417574686F72697A6174696F6E73A16A6E616D6553706163657381716F72672E69736F2E31383031332E352E316F646967657374416C676F726974686D675348412D323536"

    private val digestsByteString =
        "A2716F72672E69736F2E31383031332E352E31A3015820AF307AD59C28C5032C1A49394E0BCE7CB673A533D9AD12BD4CA980F6F12DB5BE02582076AAC7B52F938A4AEAE31D796BA5AEB2DBE0C9D96AD75B18D75AFAE875A79C76035820E2503B905A1754EF483BF5DD429C3A9BC66BDF7B3D1C080CFDEB4EC27AADEE6378186F72672E69736F2E31383031332E352E312E55746F706961A1183458208D5FFF80E966D52C123C34B07BD6546909AD7AE6BF48D736E0865A8B99853B7F"
    private val deviceKeyInfoByteString =
        "A3696465766963654B6579A4010220012158208553FBB8C982DB3F5A45D6FB12DFA04C0CF7A48F43657F203B3DD05BA8D62392225820334E2EDC3DDC0549F8C8DC79C10FA3BFE26B389F6AAD3E0603A6FCAE1BBFEA45716B6579417574686F72697A6174696F6E73A16A6E616D6553706163657381716F72672E69736F2E31383031332E352E31676B6579496E666FA22464666976652123"
    private lateinit var mobileSecurityObject: MobileSecurityObject
    private lateinit var mobileSecurityObjectCBOR: CBORObject

    @Before
    fun setUp() {
        mobileSecurityObjectCBOR =
            DecodeFromBytes(mobileSecurityObjectByteString.hexToByteArray())

        ValueDigestsTest()
        mobileSecurityObject = MobileSecurityObject.fromCBOR(mobileSecurityObjectCBOR)
    }

    //Redundant - always true
    @Test
    fun fromCBOR() {
        val mobileSecurityObjectFromCBOR = MobileSecurityObject.fromCBOR(mobileSecurityObjectCBOR)

        assertEquals(mobileSecurityObject.version, mobileSecurityObjectFromCBOR.version)
        assertEquals(mobileSecurityObject.docType, mobileSecurityObjectFromCBOR.docType)
        assertEquals(
            mobileSecurityObject.valueDigests.digests,
            mobileSecurityObjectFromCBOR.valueDigests.digests
        )
        assertEquals(mobileSecurityObject.validityInfo, mobileSecurityObjectFromCBOR.validityInfo)
        assertEquals(
            mobileSecurityObject.digestAlgorithm,
            mobileSecurityObjectFromCBOR.digestAlgorithm
        )
        assertEquals(
            mobileSecurityObject.deviceKeyInfo.deviceKey.AsPublicKey(),
            mobileSecurityObjectFromCBOR.deviceKeyInfo.deviceKey.AsPublicKey()
        )
    }

    @Test
    fun getValueDigests() {
        val digestsCBOR = DecodeFromBytes(digestsByteString.hexToByteArray())
        ValueDigestsTest()
        val valueDigest = ValueDigests.fromCBOR(digestsCBOR)

        assertEquals(
            valueDigest
                .digests
                .getValue("org.iso.18013.5.1")
                .getValue(1)
                .toHexString(),
            mobileSecurityObject
                .valueDigests
                .digests
                .getValue("org.iso.18013.5.1")
                .getValue(1)
                .toHexString()
        )

        assertEquals(
            valueDigest
                .digests
                .getValue("org.iso.18013.5.1")
                .getValue(2)
                .toHexString(),
            mobileSecurityObject
                .valueDigests
                .digests
                .getValue("org.iso.18013.5.1")
                .getValue(2)
                .toHexString()
        )
    }

    @Test
    fun getDeviceKeyInfo() {
        assertEquals(
            DecodeFromBytes(deviceKeyInfoByteString.hexToByteArray()),
            mobileSecurityObject.deviceKeyInfo.asCBOR()
        )
    }

    @Test
    fun getDocType() {
        assertEquals("org.iso.18013.5.1.mDL", mobileSecurityObject.docType)
    }

    @Test
    fun getVersion() {
        assertEquals("1.0", mobileSecurityObject.version)
    }

    @Test
    fun getValidityInfo() {
        assertEquals(
            Date.from(OffsetDateTime.parse("2019-08-08T00:00:00Z").toInstant()),
            mobileSecurityObject.validityInfo.signed
        )
        assertEquals(
            Date.from(OffsetDateTime.parse("2019-08-24T00:00:00Z").toInstant()),
            mobileSecurityObject.validityInfo.validFrom
        )
        assertEquals(
            Date.from(OffsetDateTime.parse("2029-02-28T00:00:00Z").toInstant()),
            mobileSecurityObject.validityInfo.validUntil
        )
    }

    @Test
    fun `verify status claim`() {
        val expIdx = 412L
        val expUri = "https://example.com/statuslists/1"
        val expStatus = StatusClaim(StatusListInfo(idx = expIdx, uri = expUri))
        val cborFromObj = mobileSecurityObject.copy(status = expStatus).asCBOR()
        val cborFromBytes =
            DecodeFromBytes(mobileSecurityObjectByteStringWithStatus.hexToByteArray())
        assertEquals(cborFromObj, cborFromBytes)

        val msoFromCbor = MobileSecurityObject.fromCBOR(cborFromObj)
        assertEquals(expStatus, msoFromCbor.status)
    }
}
