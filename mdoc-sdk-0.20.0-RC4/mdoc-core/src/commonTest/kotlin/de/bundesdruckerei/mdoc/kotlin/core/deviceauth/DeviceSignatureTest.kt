/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.deviceauth

import COSE.AlgorithmID
import COSE.OneKey
import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.COSEKey
import de.bundesdruckerei.mdoc.kotlin.core.common.hexToByteArray
import de.bundesdruckerei.mdoc.kotlin.crypto.CryptoUtils
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DeviceSignatureTest {

    private val deviceAuthenticationByteString = "847444657669636541757468656E7469636174696F6E83D81858828663312E308201D818584BA401022001215820E4706DE318A40D0BD8648B7907B0283F7445370241FF1FDD77F08D6A598BE90222582079767FBE391223F61DCD5E980133A035B4918F6F9DE41B3CEB8D801860DF885981830201A201F50B500000000500001000800000805F9B34FFA080A16576616C7565697465737456616C7565D818584BA40102200121582018CD31CC15E50D45C3597E2B9ECAB5771A5F61FC4290819415006251EF180C8E225820FC159B5D687BDD4580124480C3A7474ECE63234405F6126DC65653C25D573CD28258C391020F487315D10209616301013001046D646F631A200C016170706C69636174696F6E2F766E642E626C7565746F6F74682E6C652E6F6F6230081B28128B37282801021C015C1E580469736F2E6F72673A31383031333A646576696365656E676167656D656E746D646F63A20063312E30018201D818584BA4010220012158205A88D182BCE5F42EFA59943F33359D2E8A968FF289D93E5FA444B624343167FE225820B16E8CF858DDC7690407BA61D4C338237A8CFCF3DE6AA672FC60A557AA32FC6758CD91022548721591020263720102110204616301013000110206616301036E6663005102046163010157001A201E016170706C69636174696F6E2F766E642E626C7565746F6F74682E6C652E6F6F6230081B28078080BF2801021C021107C832FFF6D26FA0BEB34DFCD555D4823A1C11010369736F2E6F72673A31383031333A6E66636E6663015A172B016170706C69636174696F6E2F766E642E7766612E6E616E57030101032302001324FEC9A70B97AC9684A4E326176EF5B981C5E8533E5F00298CFCCBC35E700A6B020414756F72672E69736F2E31383031332E352E312E6D444CD81841A0"

    private lateinit var deviceAuthenticationCBOR: CBORObject
    private lateinit var deviceAuthentication: DeviceAuthentication

    private lateinit var coseKey: COSEKey

    private lateinit var deviceSignatureCBOR: CBORObject
    private lateinit var deviceSignature: DeviceSignature

    @Before
    fun setUp() {
        deviceAuthenticationCBOR = CBORObject.DecodeFromBytes(deviceAuthenticationByteString.hexToByteArray())
        deviceAuthentication = DeviceAuthentication.fromCBOR(deviceAuthenticationCBOR)

        coseKey = OneKey.generateKey(AlgorithmID.ECDSA_256)

        deviceSignature = DeviceSignature(deviceAuthentication, coseKey, CryptoUtils.CURVE_P_256)
        deviceSignatureCBOR = deviceSignature.asCBOR()
    }

    @Test
    fun validateWithRightCoseKeyShouldReturnTrue() {
        assertEquals(true, deviceSignature.isValid(deviceAuthentication, coseKey))
    }

    @Test
    fun validateWithWrongCoseKeyShouldReturnFalse() {
        val coseKey2 = OneKey.generateKey(AlgorithmID.ECDSA_256)

        assertEquals(false, deviceSignature.isValid(deviceAuthentication, coseKey2))
    }

    @Test
    fun contextTag() {
        assertEquals("Sig", deviceSignature.contextTag.toString())
    }

    @Test
    fun asCBOR() {
        assertEquals(deviceSignatureCBOR, DevAuthBase.fromCBOR(deviceSignature.asCBOR(),
            ContextTag.Sig
        ).asCBOR())
    }
}
