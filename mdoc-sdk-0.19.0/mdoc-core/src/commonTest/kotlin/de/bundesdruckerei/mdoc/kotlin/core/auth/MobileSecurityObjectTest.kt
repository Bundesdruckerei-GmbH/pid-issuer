/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.auth

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.common.toByteArray
import de.bundesdruckerei.mdoc.kotlin.toHexString
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.OffsetDateTime
import java.util.*

class MobileSecurityObjectTest{
    private val mobileSecurityObjectByteStringOld = "A56F646967657374416C676F726974686D675348412D3235366C76616C756544696765737473A16A4E616D65537061636573A2716F72672E69736F2E31383031332E352E31A3015820AF307AD59C28C5032C1A49394E0BCE7CB673A533D9AD12BD4CA980F6F12DB5BE02582076AAC7B52F938A4AEAE31D796BA5AEB2DBE0C9D96AD75B18D75AFAE875A79C7618185820580BAE0ECF9A1C4D162586D7520615E2A7AC14584967FC9BBE8373E59302CA9878186F72672E69736F2E31383031332E352E312E55746F706961A1183458208D5FFF80E966D52C123C34B07BD6546909AD7AE6BF48D736E0865A8B99853B7F696465766963654B6579A4010220012158208553FBB8C982DB3F5A45D6FB12DFA04C0CF7A48F43657F203B3DD05BA8D62392225820334E2EDC3DDC0549F8C8DC79C10FA3BFE26B389F6AAD3E0603A6FCAE1BBFEA4567646F6354797065756F72672E69736F2E31383031332E352E312E6D444C6C76616C6964697479496E666FA3667369676E6564C074323031392D30382D30385430303A30303A30305A6976616C696446726F6DC074323031392D30382D32345430303A30303A30305A6A76616C6964556E74696CC074323032392D30322D32385430303A30303A30305A"
    private val mobileSecurityObjectByteString = "A76776657273696F6E63312E306F646967657374416C676F726974686D675348412D3235366C76616C756544696765737473A2716F72672E69736F2E31383031332E352E31A3015820AF307AD59C28C5032C1A49394E0BCE7CB673A533D9AD12BD4CA980F6F12DB5BE02582076AAC7B52F938A4AEAE31D796BA5AEB2DBE0C9D96AD75B18D75AFAE875A79C7618185820580BAE0ECF9A1C4D162586D7520615E2A7AC14584967FC9BBE8373E59302CA9878186F72672E69736F2E31383031332E352E312E55746F706961A1183458208D5FFF80E966D52C123C34B07BD6546909AD7AE6BF48D736E0865A8B99853B7F696465766963654B6579A4010220012158208553FBB8C982DB3F5A45D6FB12DFA04C0CF7A48F43657F203B3DD05BA8D62392225820334E2EDC3DDC0549F8C8DC79C10FA3BFE26B389F6AAD3E0603A6FCAE1BBFEA456D6465766963654B6579496E666FA3696465766963654B6579A4010220012158208553FBB8C982DB3F5A45D6FB12DFA04C0CF7A48F43657F203B3DD05BA8D62392225820334E2EDC3DDC0549F8C8DC79C10FA3BFE26B389F6AAD3E0603A6FCAE1BBFEA45716B6579417574686F72697A6174696F6E73A16A6E616D6553706163657381716F72672E69736F2E31383031332E352E31676B6579496E666FA2246466697665212367646F6354797065756F72672E69736F2E31383031332E352E312E6D444C6C76616C6964697479496E666FA3667369676E6564C074323031392D30382D30385430303A30303A30305A6976616C696446726F6DC074323031392D30382D32345430303A30303A30305A6A76616C6964556E74696CC074323032392D30322D32385430303A30303A30305A"

    private val digestsByteString = "A2716F72672E69736F2E31383031332E352E31A3015820AF307AD59C28C5032C1A49394E0BCE7CB673A533D9AD12BD4CA980F6F12DB5BE02582076AAC7B52F938A4AEAE31D796BA5AEB2DBE0C9D96AD75B18D75AFAE875A79C76035820E2503B905A1754EF483BF5DD429C3A9BC66BDF7B3D1C080CFDEB4EC27AADEE6378186F72672E69736F2E31383031332E352E312E55746F706961A1183458208D5FFF80E966D52C123C34B07BD6546909AD7AE6BF48D736E0865A8B99853B7F"
    private val deviceKeyByteString = "A4010220012158208553FBB8C982DB3F5A45D6FB12DFA04C0CF7A48F43657F203B3DD05BA8D62392225820334E2EDC3DDC0549F8C8DC79C10FA3BFE26B389F6AAD3E0603A6FCAE1BBFEA45"
    private val deviceKeyInfoByteString = "A3696465766963654B6579A4010220012158208553FBB8C982DB3F5A45D6FB12DFA04C0CF7A48F43657F203B3DD05BA8D62392225820334E2EDC3DDC0549F8C8DC79C10FA3BFE26B389F6AAD3E0603A6FCAE1BBFEA45716B6579417574686F72697A6174696F6E73A16A6E616D6553706163657381716F72672E69736F2E31383031332E352E31676B6579496E666FA22464666976652123"



    private lateinit var mobileSecurityObject: MobileSecurityObject
    private lateinit var mobileSecurityObjectCBOR: CBORObject

    @Before
    fun setUp() {
        mobileSecurityObjectCBOR = CBORObject.DecodeFromBytes(mobileSecurityObjectByteString.toByteArray())

        ValueDigestsTest()

        mobileSecurityObject = MobileSecurityObject.fromCBOR(mobileSecurityObjectCBOR)
    }

    //Redundant - always true
    @Test
    fun fromCBOR() {
        val mobileSecurityObjectFromCBOR = MobileSecurityObject.fromCBOR(mobileSecurityObjectCBOR)

        assertEquals(mobileSecurityObject.version, mobileSecurityObjectFromCBOR.version)
        assertEquals(mobileSecurityObject.docType, mobileSecurityObjectFromCBOR.docType)
        assertEquals(mobileSecurityObject.valueDigests.digests, mobileSecurityObjectFromCBOR.valueDigests.digests)
        assertEquals(mobileSecurityObject.validityInfo, mobileSecurityObjectFromCBOR.validityInfo)
        assertEquals(mobileSecurityObject.digestAlgorithm, mobileSecurityObjectFromCBOR.digestAlgorithm)
        assertEquals(mobileSecurityObject.deviceKeyInfo.deviceKey.AsPublicKey(), mobileSecurityObjectFromCBOR.deviceKeyInfo.deviceKey.AsPublicKey())
    }

//    @Test
//    fun msoKeysTest() {
//        assertEquals("digestAlgorithm", MobileSecurityObject.KEYS.DIGEST_ALGORITHM)
//        assertEquals("valueDigests", MobileSecurityObject.KEYS.VALUE_DIGESTS)
//        assertEquals("deviceKey", MobileSecurityObject.KEYS.DEVICE_KEY)
//        assertEquals("docType", MobileSecurityObject.KEYS.DOC_TYPE)
//        assertEquals("version", MobileSecurityObject.KEYS.VERSION)
//    }

    @Test
    fun getValueDigests() {
        val digestsCBOR = CBORObject.DecodeFromBytes(digestsByteString.toByteArray())
        ValueDigestsTest()
        val valueDigest = ValueDigests.fromCBOR(digestsCBOR)

        assertEquals(valueDigest
                .digests
                .getValue("org.iso.18013.5.1")
                .getValue(1)
                .toHexString(),
                mobileSecurityObject
                        .valueDigests
                        .digests
                        .getValue("org.iso.18013.5.1")
                        .getValue(1)
                        .toHexString())

        assertEquals(valueDigest
                .digests
                .getValue("org.iso.18013.5.1")
                .getValue(2)
                .toHexString(),
                mobileSecurityObject
                        .valueDigests
                        .digests
                        .getValue("org.iso.18013.5.1")
                        .getValue(2)
                        .toHexString())
    }

    @Test
    fun getDeviceKeyInfo() {
        assertEquals(CBORObject.DecodeFromBytes(deviceKeyInfoByteString.toByteArray()), mobileSecurityObject.deviceKeyInfo?.asCBOR())
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
        assertEquals(Date.from(OffsetDateTime.parse("2019-08-08T00:00:00Z").toInstant()), mobileSecurityObject.validityInfo.signed)
        assertEquals(Date.from(OffsetDateTime.parse("2019-08-24T00:00:00Z").toInstant()), mobileSecurityObject.validityInfo.validFrom)
        assertEquals(Date.from(OffsetDateTime.parse("2029-02-28T00:00:00Z").toInstant()), mobileSecurityObject.validityInfo.validUntil)
    }



}
