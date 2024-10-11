/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.auth

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.core.auth.dto.ValidityRange
import de.bundesdruckerei.mdoc.kotlin.core.common.ICBORable
import de.bundesdruckerei.mdoc.kotlin.core.tdate
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*

data class ValidityInfo(
    val signed: tdate,
    val validFrom: tdate,
    val validUntil: tdate,
    val expectedUpdate: tdate?
) :
    ICBORable {
    constructor(cborObject: CBORObject) :
        this(
            toDate(cborObject["signed"]),
            toDate(cborObject["validFrom"]),
            toDate(cborObject["validUntil"]),
            cborObject["expectedUpdate"]?.let { toDate(it) }
        )

    constructor(
        signed: Instant,
        validityRange: ValidityRange
    ) : this(
        signed = Date.from(signed.truncatedTo(ChronoUnit.SECONDS)),
        validFrom = Date.from(validityRange.validFrom),
        validUntil = Date.from(validityRange.validUntil),
        expectedUpdate = validityRange.expectedUpdate?.let { Date.from(it) }
    )

    companion object {
        fun toDate(tdate: CBORObject): Date =
            Date.from(OffsetDateTime.parse(tdate.AsString()).toInstant())
    }

    fun asBytes(): ByteArray = asCBOR().EncodeToBytes()

    fun toValidityRange() = ValidityRange(
        validFrom = validFrom.toInstant(),
        validUntil = validUntil.toInstant(),
        expectedUpdate = expectedUpdate?.toInstant()
    )

    override fun asCBOR(): CBORObject {
        return CBORObject.NewMap().apply {
            Set("signed", signed)
            Set("validFrom", validFrom)
            Set("validUntil", validUntil)
            expectedUpdate?.let {
                Set("expectedUpdate", it)
            }
        }
    }
}
