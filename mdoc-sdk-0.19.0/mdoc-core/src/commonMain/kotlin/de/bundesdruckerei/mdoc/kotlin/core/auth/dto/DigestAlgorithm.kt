/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.auth.dto

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.tstr

@JvmInline
value class DigestAlgorithm(val value: tstr) {

    fun asCBOR(): CBORObject = CBORObject.FromObject(value)

    override fun toString() = value

    companion object {
        val SHA_256 = DigestAlgorithm("SHA-256")
        val SHA_384 = DigestAlgorithm("SHA-384")
        val SHA_512 = DigestAlgorithm("SHA-512")

        fun fromCBOR(cborObject: CBORObject) = DigestAlgorithm(cborObject.AsString())
    }
}
