/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.auth.dto

import java.time.Instant
import java.time.temporal.ChronoUnit

class ValidityRange @JvmOverloads constructor(
    validFrom: Instant,
    validUntil: Instant,
    expectedUpdate: Instant? = null
) : ClosedRange<Instant> by (validFrom.truncatedTo(ChronoUnit.SECONDS)..validUntil.truncatedTo(ChronoUnit.SECONDS)) {

    inline val validFrom get() = start
    inline val validUntil get() = endInclusive

    init {
        require(!isEmpty()) {
            "${::validFrom.name} `$validFrom` must be before ${::validUntil.name} `$validUntil`."
        }
    }

    val expectedUpdate: Instant? = expectedUpdate?.truncatedTo(ChronoUnit.SECONDS)

    @JvmSynthetic
    operator fun component1() = validFrom

    @JvmSynthetic
    operator fun component2() = validUntil

    @JvmSynthetic
    operator fun component3() = expectedUpdate

    @JvmSynthetic
    @Suppress("unused")
    fun copy(
        validFrom: Instant = this.validFrom,
        validUntil: Instant = this.validUntil,
        expectedUpdate: Instant? = this.expectedUpdate
    ) = ValidityRange(
        validFrom = validFrom,
        validUntil = validUntil,
        expectedUpdate = expectedUpdate
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ValidityRange

        if (validFrom != other.validFrom) return false
        if (validUntil != other.validUntil) return false
        if (expectedUpdate != other.expectedUpdate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = validFrom.hashCode()
        result = 31 * result + validUntil.hashCode()
        result = 31 * result + (expectedUpdate?.hashCode() ?: 0)
        return result
    }

    override fun toString() = "${ValidityRange::class.simpleName}(" +
            "${::validFrom.name}=$validFrom, " +
            "${::validUntil.name}=$validUntil, " +
            "${::expectedUpdate.name}=$expectedUpdate" +
            ")"
}
