/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common

import java.time.Duration
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class DurationToSecondsSerializer : KSerializer<Duration> {

    private val delegate = Long.serializer()

    override val descriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: Duration) {
        delegate.serialize(encoder, value.toSeconds())
    }

    override fun deserialize(decoder: Decoder): Duration {
        val deserialized = delegate.deserialize(decoder)
        return Duration.ofSeconds(deserialized.toLong())
    }
}
