/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.signing

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.lang.IllegalArgumentException
import java.security.KeyPair
import java.security.KeyStore
import java.security.Signature
import kotlin.test.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import tests.TestCa
import tests.TestCa.Algorithm.SHA256WithECDSA
import tests.TestCa.Algorithm.SHA384WithECDSA
import tests.TestCa.Algorithm.SHA512WithECDSA

class Pkcs12SignerTest {

    private val testCa = TestCa(SHA256WithECDSA)

    @Test
    fun `given a keystore with P256 key when signing then the signature is valid`() {
        val (keystoreStream, keys) = keystoreStreamAndKeys(SHA256WithECDSA)
        val signer = Pkcs12Signer(keystoreStream, "testtest")

        val result = signer.sign(ByteArray(16))

        val signature = Signature.getInstance("SHA256WithECDSA")
        signature.initVerify(keys.public)
        signature.update(ByteArray(16))
        assertTrue(signature.verify(result))
    }

    @Test
    fun `given a keystore with P384 key when signing then the signature is valid`() {
        val (keystoreStream, keys) = keystoreStreamAndKeys(SHA384WithECDSA)
        val signer = Pkcs12Signer(keystoreStream, "testtest")

        val result = signer.sign(ByteArray(16))

        val signature = Signature.getInstance("SHA384WithECDSA")
        signature.initVerify(keys.public)
        signature.update(ByteArray(16))
        assertTrue(signature.verify(result))
    }

    @Test
    fun `given a keystore with P521 key when signing then the signature is valid`() {
        val (keystoreStream, keys) = keystoreStreamAndKeys(SHA512WithECDSA)
        val signer = Pkcs12Signer(keystoreStream, "testtest")

        val result = signer.sign(ByteArray(16))

        val signature = Signature.getInstance("SHA512WithECDSA")
        signature.initVerify(keys.public)
        signature.update(ByteArray(16))
        assertTrue(signature.verify(result))
    }

    @Test
    fun `given a keystore with a cert chain when retrieving the key material then it contains the chain`() {
        val (keystoreStream, keys) = keystoreStreamAndKeys(SHA256WithECDSA)
        val signer = Pkcs12Signer(keystoreStream, "testtest")

        val keyMaterial = signer.keys

        assertThat(keyMaterial.certificates.size).isEqualTo(2)
        assertThat(keyMaterial.certificates.first().publicKey).isEqualTo(keys.public)
        assertThat(keyMaterial.certificates.last()).isEqualTo(testCa.cert)
    }

    @Test
    fun `given a keystore without key entry when passed to Pkcs12Signer then it throws IllegalArgumentException`() {
        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(null, null)
        assertThrows<IllegalArgumentException> { Pkcs12Signer(toInputStream(keyStore), "testtest") }
    }

    private fun keystoreStreamAndKeys(algorithm: TestCa.Algorithm): Pair<InputStream, KeyPair> {
        val (cert, keyPair) = testCa.generate(algorithm, "test")
        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(null, null)
        keyStore.setKeyEntry(
            "entry",
            keyPair.private,
            "testtest".toCharArray(),
            arrayOf(cert, testCa.cert)
        )
        return Pair(toInputStream(keyStore), keyPair)
    }

    private fun toInputStream(keyStore: KeyStore): ByteArrayInputStream {
        val out = ByteArrayOutputStream()
        keyStore.store(out, "testtest".toCharArray())
        return ByteArrayInputStream(out.toByteArray())
    }
}
