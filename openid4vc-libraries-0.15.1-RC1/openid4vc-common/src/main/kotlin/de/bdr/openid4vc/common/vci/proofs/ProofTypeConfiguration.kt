/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vci.proofs

import kotlin.reflect.full.createType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer

interface ProofTypeConfiguration {
    val proofType: ProofType
    val signingAlgValuesSupported: List<String>
}

typealias ProofTypesSupported =
    @Serializable(with = SupportedProofTypesSerializer::class)
    Map<ProofType, ProofTypeConfiguration>

object SupportedProofTypesSerializer : KSerializer<Map<ProofType, ProofTypeConfiguration>> {

    @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("SupportedProofTypeSerializer", StructureKind.MAP)

    override fun serialize(encoder: Encoder, value: Map<ProofType, ProofTypeConfiguration>) {

        val jsonEncoder =
            encoder as? JsonEncoder
                ?: throw SerializationException("This class can only be serialized to JSON")

        val jsonObject = buildJsonObject {
            value.forEach { (proofType, proofConfig) ->
                put(
                    proofType.value,
                    jsonEncoder.json.encodeToJsonElement(
                        serializer(proofConfig::class.createType()),
                        proofConfig,
                    ),
                )
            }
        }

        jsonEncoder.encodeJsonElement(jsonObject)
    }

    @SuppressWarnings("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): Map<ProofType, ProofTypeConfiguration> {
        val input =
            decoder as? JsonDecoder
                ?: throw SerializationException("This class can only be deserialized from JSON")

        val list: List<ProofTypeConfiguration?> =
            input.decodeJsonElement().jsonObject.entries.mapNotNull { entry ->
                val proofType = ProofTypeRegistry.registry[entry.key]

                if (proofType == null) {
                    require(input.json.configuration.ignoreUnknownKeys) {
                        "unknown proof type: ${entry.key}"
                    }
                    null
                } else {
                    input.json.decodeFromJsonElement(
                        serializer(proofType.proofTypeConfigurationClass.createType())
                            as KSerializer<ProofTypeConfiguration>,
                        entry.value,
                    )
                }
            }

        return list.filterNotNull().associateBy({ it.proofType }, { it })
    }
}
