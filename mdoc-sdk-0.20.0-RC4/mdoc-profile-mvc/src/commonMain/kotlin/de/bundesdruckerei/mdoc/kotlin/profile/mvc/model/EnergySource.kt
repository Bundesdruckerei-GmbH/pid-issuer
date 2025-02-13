package de.bundesdruckerei.mdoc.kotlin.profile.mvc.model

import de.bundesdruckerei.mdoc.kotlin.core.uint
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = EnergySourceSerializer::class)
enum class EnergySource(val identifier: uint) {
    Petrol(10),
    PetrolE5(11),
    PetrolE10(12),
    Ethanol(15),
    EthanolE85(16),
    Mixture(19),
    Diesel(20),
    Biodiesel(21),
    ED95(22),
    LPG(30),
    CNG(40),
    Biomethane(44),
    Hydrogen(50),
    H2NG(55),
    LNG(60),
    Other(90),
    CompressedAir(91),
    Electricity(95);

    companion object {
        fun findByIdentifier(identifier: uint): EnergySource =
            entries.find { it.identifier == identifier } ?: Other
    }
}

object EnergySourceSerializer : KSerializer<EnergySource> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("energy_source", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: EnergySource) {
        encoder.encodeLong(value.identifier)
    }

    override fun deserialize(decoder: Decoder): EnergySource {
        val identifier = decoder.decodeLong()
        return EnergySource.findByIdentifier(identifier)
    }
}
