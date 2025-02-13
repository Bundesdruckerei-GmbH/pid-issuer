package de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.de

import de.bundesdruckerei.mdoc.kotlin.core.common.CBOR_TAG_FULL_DATE
import de.bundesdruckerei.mdoc.kotlin.core.common.serializer.LocalDateFullDateSerializer
import de.bundesdruckerei.mdoc.kotlin.core.tstr
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.ValueTags
import java.time.LocalDate

@ExperimentalUnsignedTypes
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BasicVehicleInfoNat(
    @SerialName("type")
    val type: tstr? = null,
    @SerialName("variant")
    val variant: tstr? = null,
    @SerialName("version")
    val version: tstr? = null,
    @SerialName("manufacturer")
    val manufacturer: tstr? = null,
    @SerialName("manufacturer_key")
    val manufacturerKey: tstr? = null,
    @SerialName("key_type")
    val keyType: tstr? = null,
    @SerialName("key_variant_version")
    val keyVariantVersion: tstr? = null,
    @SerialName("check_digit_type_variant_version")
    val checkDigitTypeVariantVersion: tstr? = null,
    @SerialName("check_digit_vehicle_identification_number")
    val checkDigitVehicleIdentificationNumber: tstr? = null,
    @SerialName("vehicle_category_body")
    val vehicleCategoryBody: tstr? = null,
    @SerialName("key_body")
    val keyBody: tstr? = null,
    @Serializable(LocalDateFullDateSerializer::class)
    @ValueTags(CBOR_TAG_FULL_DATE)
    @SerialName("type_approval_date")
    val typeApprovalDate: LocalDate? = null,
    @SerialName("approval_type")
    val approvalType: tstr? = null,
    @SerialName("colours")
    val colours: List<tstr>? = null,
    @SerialName("colour_text")
    val colourText: List<tstr>? = null,
)


