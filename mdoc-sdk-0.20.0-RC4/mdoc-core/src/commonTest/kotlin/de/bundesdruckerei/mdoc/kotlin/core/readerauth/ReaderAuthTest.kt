/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.readerauth

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.COSEKey
import de.bundesdruckerei.mdoc.kotlin.test.Data
import de.bundesdruckerei.mdoc.kotlin.core.common.hexToByteArray
import de.bundesdruckerei.mdoc.kotlin.core.common.toHex
import de.bundesdruckerei.mdoc.kotlin.crypto.CertUtils
import de.bundesdruckerei.mdoc.kotlin.crypto.CryptoUtils
import de.bundesdruckerei.mdoc.kotlin.generatePrivateKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import java.math.BigInteger
import java.security.cert.X509Certificate

class ReaderAuthTest {

    private lateinit var readerAuthentication: ReaderAuthentication
    private lateinit var readerAuthenticationKey: COSEKey
    private val readerPrivateKeyD = "de3b4b9e5f72dd9b58406ae3091434da48a6f9fd010d88fcb0958e2cebec947c"
    private lateinit var curveName: String

    private val readerCertificateByteString = "3082019030820137a003020102021430d747795405d564b7ac48be6f364ae2c774f2fc300a06082a8648ce3d04030230163114301206035504030c0b72656164657220726f6f74301e170d3230313030313030303030305a170d3239303932393030303030305a30163114301206035504030c0b72656164657220726f6f743059301306072a8648ce3d020106082a8648ce3d030107034200043643293832e0a480de592df0708fe25b6b923f6397ab39a8b1b7444593adb89c77b7e9c28cf48d6d187b43c9bf7b9c2c5c5ef22f329e44e7a91b4745b7e2063aa3633061301c0603551d1f041530133011a00fa00d820b6578616d706c652e636f6d301d0603551d0e04160414cfb7a881baea5f32b6fb91cc29590c50dfac416e300e0603551d0f0101ff04040302010630120603551d130101ff040830060101ff020100300a06082a8648ce3d0403020347003044022018ac84baf991a614fb25e76241857b7fd0579dfe8aed8ac7f130675490799930022077f46f00b4af3e014d253e0edcc9f146a75a6b1bdfe33e9fa72f30f0880d5237"
    private lateinit var readerCertificate: X509Certificate
    private val readerCertificateDSByteString = "308201B330820158A00302010202147552715F6ADD323D4934A1BA175DC945755D8B50300A06082A8648CE3D04030230163114301206035504030C0B72656164657220726F6F74301E170D3230313030313030303030305A170D3233313233313030303030305A3011310F300D06035504030C067265616465723059301306072A8648CE3D020106082A8648CE3D03010703420004F8912EE0F912B6BE683BA2FA0121B2630E601B2B628DFF3B44F6394EAA9ABDBCC2149D29D6FF1A3E091135177E5C3D9C57F3BF839761EED02C64DD82AE1D3BBFA38188308185301C0603551D1F041530133011A00FA00D820B6578616D706C652E636F6D301D0603551D0E04160414F2DFC4ACAFC5F30B464FADA20BFCD533AF5E07F5301F0603551D23041830168014CFB7A881BAEA5F32B6FB91CC29590C50DFAC416E300E0603551D0F0101FF04040302078030150603551D250101FF040B3009060728818C5D050106300A06082A8648CE3D0403020349003046022100FB9EA3B686FD7EA2F0234858FF8328B4EFEF6A1EF71EC4AAE4E307206F9214930221009B94F0D739DFA84CCA29EFED529DD4838ACFD8B6BEE212DC6320C46FEB839A35"
    private lateinit var readerCertificateDS: X509Certificate

    private lateinit var readerAuth: ReaderAuth

    private val readerAuthExpectedByteString = "8443a10126a118215901b7308201b330820158a00302010202147552715f6add323d4934a1ba175dc945755d8b50300a06082a8648ce3d04030230163114301206035504030c0b72656164657220726f6f74301e170d3230313030313030303030305a170d3233313233313030303030305a3011310f300d06035504030c067265616465723059301306072a8648ce3d020106082a8648ce3d03010703420004f8912ee0f912b6be683ba2fa0121b2630e601b2b628dff3b44f6394eaa9abdbcc2149d29d6ff1a3e091135177e5c3d9c57f3bf839761eed02c64dd82ae1d3bbfa38188308185301c0603551d1f041530133011a00fa00d820b6578616d706c652e636f6d301d0603551d0e04160414f2dfc4acafc5f30b464fada20bfcd533af5e07f5301f0603551d23041830168014cfb7a881baea5f32b6fb91cc29590c50dfac416e300e0603551d0f0101ff04040302078030150603551d250101ff040b3009060728818c5d050106300a06082a8648ce3d0403020349003046022100fb9ea3b686fd7ea2f0234858ff8328b4efef6a1ef71ec4aae4e307206f9214930221009b94f0d739dfa84cca29efed529dd4838acfd8b6bee212dc6320c46feb839a35f658401f3400069063c189138bdcd2f631427c589424113fc9ec26cebcacacfcdb9695d28e99953becabc4e30ab4efacc839a81f9159933d192527ee91b449bb7f80bf"
    @Before
    fun setUp() {
        readerAuthentication = ReaderAuthentication.fromTaggedCBOR(Data.ReaderAuthentication.cborObjTagged)

        curveName = CryptoUtils.CURVE_P_256
        readerCertificate = CertUtils.decodeCertificates(readerCertificateByteString.hexToByteArray()).certificates[0] as X509Certificate
        readerCertificateDS = CertUtils.decodeCertificates(readerCertificateDSByteString.hexToByteArray()).certificates[0] as X509Certificate

        val readerPrKey = generatePrivateKey(BigInteger(readerPrivateKeyD, 16), CryptoUtils.CURVE_P_256)
        readerAuthenticationKey = COSEKey(null, readerPrKey)

        readerAuth = ReaderAuth(readerAuthentication, readerAuthenticationKey, curveName, readerCertificateDS)
    }

    //Can't properly test because can't find reader's private key in ISO Standard...
    @Test
    fun validate() {
        val publicKey = COSEKey(readerCertificateDS.publicKey, null)
        assertEquals(false, readerAuth.validate(readerAuthentication, publicKey))
    }

    //bytes will always be different because signature is always different
    @Test
    fun asBytes() {
        assertNotEquals(readerAuthExpectedByteString, readerAuth.asBytes().toHex())
    }

    //CBOR will always be different because signature is always different
    @Test
    fun asCBOR() {
        val cbor = CBORObject.DecodeFromBytes(readerAuthExpectedByteString.hexToByteArray())
        assertNotEquals(cbor, readerAuth.asCBOR())
    }

    @Test
    fun constructorWithCbor() {
        val cbor = readerAuth.asCBOR()
        assertEquals(readerAuth.asCBOR(), ReaderAuth(cbor).asCBOR())
    }

    @Test
    fun getCertificate() {
        assertEquals(readerCertificateDS.encoded.toHex(), readerAuth.message.unprotectedAttributes[33].GetByteString().toHex())
    }

    @Test
    fun getProtectedHeader() {
        assertEquals(-7, readerAuth.message.protectedAttributes[1].AsInt64Value())
    }

    @Test
    fun getMessage() {
        assertEquals(readerAuth.asCBOR(), readerAuth.message.EncodeToCBORObject())
    }
}
