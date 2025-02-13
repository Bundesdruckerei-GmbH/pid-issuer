package de.bundesdruckerei.mdoc.kotlin.profile.mvc.model

import de.bundesdruckerei.mdoc.kotlin.core.uint
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ColourSerializer::class)
enum class Colour(val identifier: uint) {
    White(1),
    Yellow(2),
    Orange(3),
    Red(4),
    Violet(5),
    Blue(6),
    Green(7),
    Grey(8),
    Brown(9),
    Black(10);

    companion object {
        fun findByIdentifier(identifier: uint): Colour =
            entries.find { it.identifier == identifier } ?: Black
    }
}

object ColourSerializer : KSerializer<Colour> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FuelType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Colour) {
        encoder.encodeLong(value.identifier)
    }

    override fun deserialize(decoder: Decoder): Colour {
        val identifier = decoder.decodeLong()
        return Colour.findByIdentifier(identifier)
    }
}
