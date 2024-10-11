/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.deviceauth

import de.bundesdruckerei.mdoc.kotlin.core.common.log
import de.bundesdruckerei.mdoc.kotlin.core.common.toByteArray
import de.bundesdruckerei.mdoc.kotlin.crypto.CryptoUtils
import de.bundesdruckerei.mdoc.kotlin.generatePrivateKey
import de.bundesdruckerei.mdoc.kotlin.generatePublicFromPrivateKey
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert
import org.junit.Test
import java.math.BigInteger
import java.nio.charset.StandardCharsets

/*
EMacKey shall be derived using HKDF as defined in RFC 5869 with the following parameters:
— Hash: SHA-256,
— IKM: ZAB,
— salt: SHA-256(SessionTranscriptBytes),
— info: “EMacKey” (encoded as a UTF-8 string),
— L: 32 octets.


The MAC value is contained in the tag element within DeviceAuth in an untagged COSE_Mac0 structure
as defined in RFC 8152 and identified as DeviceMac. Within the COSE_Mac0 structure, the payload shall
have a null value. The detached content is DeviceAuthenticationBytes. The ‘external_aad’ field shall be
a bytestring of size zero.
The alg element (RFC 8152) shall be included as an element in the protected header. Other elements
should not be present in the protected header.
RFC 8152 describes the algorithm identifiers that shall be used in the alg element. “HMAC 256/256”
(HMAC with SHA-256) shall be used.
9.1.3.6 mdoc ECDSA / EdDSA Authenticatio

*/
class MacKeyTest
{

    private val key_x = "96313d6c63e24e3372742bfdb1a33ba2c897dcd68ab8c753e4fbd48dca6b7f9a"
    private val key_y = "1fb3269edd418857de1b39a4e4a44b92fa484caa722c228288f01d0c03a2c3d6"
    private val key_d = "6ed542ad4783f0b18c833fadf2171273a35d969c581691ef704359cc7cf1e8c0"

    private val mac_Zab =  "78d98a86fbbb82895874bfafcc161ba69f9b77662172c74b3b0d4643276cf991"
    private val mac_key =  "dc2b9566fdaaae3c06baa40993cd0451aeba15e7677ef5305f6531f3533c35dd"

    private val sessionTranscriptBytes = "d81859024183d8185858a20063312e30018201d818584ba4010220012158205a88d182bce5f42efa59943f33359d2e8a968ff289d93e5fa444b624343167fe225820b16e8cf858ddc7690407ba61d4c338237a8cfcf3de6aa672fc60a557aa32fc67d818584ba40102200121582060e3392385041f51403051f2415531cb56dd3f999c71687013aac6768bc8187e225820e58deb8fdbe907f7dd5368245551a34796f7d2215c440c339bb0f7b67beccdfa8258c391020f487315d10209616301013001046d646f631a200c016170706c69636174696f6e2f766e642e626c7565746f6f74682e6c652e6f6f6230081b28128b37282801021c015c1e580469736f2e6f72673a31383031333a646576696365656e676167656d656e746d646f63a20063312e30018201d818584ba4010220012158205a88d182bce5f42efa59943f33359d2e8a968ff289d93e5fa444b624343167fe225820b16e8cf858ddc7690407ba61d4c338237a8cfcf3de6aa672fc60a557aa32fc6758cd91022548721591020263720102110204616301013000110206616301036e6663005102046163010157001a201e016170706c69636174696f6e2f766e642e626c7565746f6f74682e6c652e6f6f6230081b28078080bf2801021c021107c832fff6d26fa0beb34dfcd555d4823a1c11010369736f2e6f72673a31383031333a6e66636e6663015a172b016170706c69636174696f6e2f766e642e7766612e6e616e57030101032302001324fec9a70b97ac9684a4e326176ef5b981c5e8533e5f00298cfccbc35e700a6b020414"

    @Test
    fun fromCBOR()
    {
        var privateKey = generatePrivateKey(BigInteger(key_d, 16), CryptoUtils.CURVE_P_256)
        var publicKey = generatePublicFromPrivateKey(privateKey)



        val salt = CryptoUtils.digestSHA256(sessionTranscriptBytes.toByteArray())
        val calculatedMacKey = CryptoUtils.deriveSessionKeyFromSharedSecret(mac_Zab.toByteArray(), salt, "EMacKey".toByteArray(StandardCharsets.UTF_8))
        Assert.assertEquals(Hex.toHexString(calculatedMacKey), mac_key)
        log.d("macKey : ${Hex.toHexString(calculatedMacKey)}")
    }

}