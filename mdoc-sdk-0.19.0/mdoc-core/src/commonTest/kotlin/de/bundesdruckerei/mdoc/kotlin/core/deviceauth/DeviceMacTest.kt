/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.deviceauth

import COSE.AlgorithmID
import COSE.OneKey
import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.COSEKey
import de.bundesdruckerei.mdoc.kotlin.core.common.toByteArray
import de.bundesdruckerei.mdoc.kotlin.crypto.CryptoUtils
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DeviceMacTest {

    private val deviceAuthenticationByteString = "d818590271847444657669636541757468656e7469636174696f6e83d8185858a20063312e30018201d818584ba4010220012158205a88d182bce5f42efa59943f33359d2e8a968ff289d93e5fa444b624343167fe225820b16e8cf858ddc7690407ba61d4c338237a8cfcf3de6aa672fc60a557aa32fc67d818584ba40102200121582060e3392385041f51403051f2415531cb56dd3f999c71687013aac6768bc8187e225820e58deb8fdbe907f7dd5368245551a34796f7d2215c440c339bb0f7b67beccdfa8258c391020f487315d10209616301013001046d646f631a200c016170706c69636174696f6e2f766e642e626c7565746f6f74682e6c652e6f6f6230081b28128b37282801021c015c1e580469736f2e6f72673a31383031333a646576696365656e676167656d656e746d646f63a20063312e30018201d818584ba4010220012158205a88d182bce5f42efa59943f33359d2e8a968ff289d93e5fa444b624343167fe225820b16e8cf858ddc7690407ba61d4c338237a8cfcf3de6aa672fc60a557aa32fc6758cd91022548721591020263720102110204616301013000110206616301036e6663005102046163010157001a201e016170706c69636174696f6e2f766e642e626c7565746f6f74682e6c652e6f6f6230081b28078080bf2801021c021107c832fff6d26fa0beb34dfcd555d4823a1c11010369736f2e6f72673a31383031333a6e66636e6663015a172b016170706c69636174696f6e2f766e642e7766612e6e616e57030101032302001324fec9a70b97ac9684a4e326176ef5b981c5e8533e5f00298cfccbc35e700a6b020414756f72672e69736f2e31383031332e352e312e6d444cd81841a0"

    private lateinit var deviceAuthenticationCBOR: CBORObject
    private lateinit var deviceAuthentication: DeviceAuthentication

    private lateinit var coseKey: COSEKey
    private lateinit var eMacKey: EMacKey

    private lateinit var deviceMac: DeviceMac

    @Before
    fun setUp() {

        deviceAuthenticationCBOR = CBORObject.DecodeFromBytes(deviceAuthenticationByteString.toByteArray())
        deviceAuthentication = DeviceAuthentication.fromTaggedCBOR(deviceAuthenticationCBOR)

        coseKey = OneKey.generateKey(AlgorithmID.ECDSA_256)

        eMacKey = "dc2b9566fdaaae3c06baa40993cd0451aeba15e7677ef5305f6531f3533c35dd".toByteArray()

        deviceMac = DeviceMac(deviceAuthentication, eMacKey, CryptoUtils.CURVE_P_256)
    }

    @Test
    fun validateWithRightMacKeyShouldReturnTrue() {
        assertEquals(true, deviceMac.isValid(deviceAuthentication, eMacKey))

    }

    @Test
    fun validateWithWrongMacKeyShouldReturnFalse() {
        val coseKey2 = OneKey.generateKey(AlgorithmID.ECDSA_256)
        val eMacKey2 = "cc2b9566fdaaae3c06baa40993cd0451aeba15e7677ef5305f6531f3533c35dd".toByteArray()

        assertEquals(false, deviceMac.isValid(deviceAuthentication, eMacKey2))
    }

    @Test
    fun contextTag() {
        assertEquals("Mac", deviceMac.contextTag.toString())
    }


}