package de.bundesdruckerei.mdoc.kotlin.profile.mvc

import com.upokecenter.cbor.CBORObject
import de.bundesdruckerei.mdoc.kotlin.profile.BooleanItem
import de.bundesdruckerei.mdoc.kotlin.profile.Charset
import de.bundesdruckerei.mdoc.kotlin.profile.CustomItem
import de.bundesdruckerei.mdoc.kotlin.profile.Namespace
import de.bundesdruckerei.mdoc.kotlin.profile.Profile
import de.bundesdruckerei.mdoc.kotlin.profile.TdateItem
import de.bundesdruckerei.mdoc.kotlin.profile.TstrEnumItem
import de.bundesdruckerei.mdoc.kotlin.profile.TstrItem
import de.bundesdruckerei.mdoc.kotlin.profile.UintItem
import de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.BasicVehicleInfo
import de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.EngineInfo
import de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.MassInfo
import de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.RegistrationHolders
import de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.RegistrationOwners
import de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.SeatingInfo
import de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.TrailerMassInfo
import de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.de.AxisInfoNat
import de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.de.BasicVehicleInfoNat
import de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.de.DimensionInfoNat
import de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.de.EngineInfoNat
import de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.de.EnvironmentInfoNat
import de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.de.Indication
import de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.de.MassInfoNat
import de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.de.RegistrationHoldersNat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray

/**
 * Profile for the Technical Specification ISO/IEC WD 7367:2024(E) 2024-09-30 augmented by
 * DE domestic namespace.
 */
object MVC {

    private var alpha2Codes = arrayListOf("DE") // and so on

    @ExperimentalUnsignedTypes
    @ExperimentalSerializationApi
    val profile = Profile(
        "org.iso.7367.1.mVC",
        arrayListOf(
            Namespace(
                OrgIso232201.NAME,
                presence = true,
                items = arrayListOf(
                    TdateItem(OrgIso232201.ISSUE_DATE_IDENTIFIER, true, null),
                    TdateItem(OrgIso232201.EXPIRY_DATE, false, null),
                    TstrItem(OrgIso232201.ISSUING_AUTHORITY_IDENTIFIER_LATIN1, true, null, null, 150, Charset.LATIN1),
                    TstrItem(OrgIso232201.ISSUING_AUTHORITY_IDENTIFIER_UNICODE, false, null, null, 150, Charset.UNICODE),
                    TstrEnumItem(OrgIso232201.ISSUING_COUNTRY_IDENTIFIER, true, null, alpha2Codes),
                    TstrItem(OrgIso232201.ISSUING_JURISDICTION_IDENTIFIER, false, fun(_: CBORObject): Boolean {
                        return true
                    }, null, null, null)
                )
            ),
            Namespace(
                OrgIso73671.NAME,
                presence = true,
                items = arrayListOf(
                    TstrItem(OrgIso73671.REGISTRATION_NUMBER_IDENTIFIER, true, null, null, null, null),
                    TstrItem(OrgIso73671.REGISTRATION_NUMBER_TYPE_IDENTIFIER, false, null, null, null, null),
                    TstrItem(OrgIso73671.REGISTER_NUMBER_IDENTIFIER, false, null, null, null, null),
                    TdateItem(OrgIso73671.DATE_OF_REGISTRATION_IDENTIFIER, true, null),
                    TdateItem(OrgIso73671.DATE_OF_FIRST_REGISTRATION_IDENTIFIER, false, null),
                    TdateItem(OrgIso73671.APPROVAL_DATE_TECHNICAL_INSPECTION_IDENTIFIER, false, null),
                    TdateItem(OrgIso73671.EXPIRY_DATE_TECHNICAL_INSPECTION_IDENTIFIER, false, null),
                    TstrItem(OrgIso73671.VEHICLE_IDENTIFICATION_NUMBER_IDENTIFIER, true, null, null, null, null),
                    TstrItem(OrgIso73671.UN_DISTINGUISHING_SIGN_IDENTIFIER, true, null, null, null, null),
                    TstrItem(OrgIso73671.YEAR_OF_MANUFACTURING, false, null, null, null, null),
                    CustomItem(OrgIso73671.REGISTERED_USER_IDENTIFIER, true, fun(data: CBORObject): Boolean {
                        try {
                            Cbor.decodeFromByteArray<RegistrationHolders>(data.EncodeToBytes())
                        } catch (e: Exception) {
                            return false
                        }
                        return true
                    }),
                    CustomItem(OrgIso73671.REGISTERED_OWNER_IDENTIFIER, false, fun(data: CBORObject): Boolean {
                        try {
                            Cbor.decodeFromByteArray<RegistrationOwners>(data.EncodeToBytes())
                        } catch (e: Exception) {
                            return false
                        }
                        return true
                    }),
                    CustomItem(OrgIso73671.BASIC_VEHICLE_INFO_IDENTIFIER, true, fun(data: CBORObject): Boolean {
                        try {
                            Cbor.decodeFromByteArray<BasicVehicleInfo>(data.EncodeToBytes())
                        } catch (e: Exception) {
                            return false
                        }
                        return true
                    }),
                    CustomItem(OrgIso73671.MASS_INFO_IDENTIFIER, true, fun(data: CBORObject): Boolean {
                        try {
                            Cbor.decodeFromByteArray<MassInfo>(data.EncodeToBytes())
                        } catch (e: Exception) {
                            return false
                        }
                        return true
                    }),
                    CustomItem(OrgIso73671.TRAILER_MASS_INFO_IDENTIFIER, false, fun(data: CBORObject): Boolean {
                        try {
                            Cbor.decodeFromByteArray<TrailerMassInfo>(data.EncodeToBytes())
                        } catch (e: Exception) {
                            return false
                        }
                        return true
                    }),
                    CustomItem(OrgIso73671.ENGINE_INFO_IDENTIFIER, false, fun(data: CBORObject): Boolean {
                        try {
                            Cbor.decodeFromByteArray<EngineInfo>(data.EncodeToBytes())
                        } catch (e: Exception) {
                            return false
                        }
                        return true
                    }),
                    CustomItem(OrgIso73671.SEATING_INFO_IDENTIFIER, false, fun(data: CBORObject): Boolean {
                        try {
                            Cbor.decodeFromByteArray<SeatingInfo>(data.EncodeToBytes())
                        } catch (e: Exception) {
                            return false
                        }
                        return true
                    }),
                    TstrItem(OrgIso73671.ISSUING_JURISDICTION_IDENTIFIER, false, fun(_: CBORObject): Boolean {
                        return true
                    }, null, null, null),
                )
            ),
            Namespace(
                OrgIso73671DE.NAME,
                presence = false,
                items = arrayListOf(
                    BooleanItem(OrgIso73671DE.IS_MAIN_IDENTIFIER, true, null),
                    CustomItem(OrgIso73671DE.INDICATION_IDENTIFIER, false, fun(data: CBORObject): Boolean {
                        try {
                            Cbor.decodeFromByteArray<Indication>(data.EncodeToBytes())
                        } catch (e: Exception) {
                            return false
                        }
                        return true
                    }),
                    TstrItem(OrgIso73671DE.REMARKS_EXCEPTIONS_IDENTIFIER, false, null, null, null, Charset.LATIN1),
                    TstrItem(
                        OrgIso73671DE.TECHNICAL_INSPECTION_TRANSMITTING_AUTHORITY_IDENTIFIER,
                        false,
                        null,
                        null,
                        null,
                        Charset.LATIN1
                    ),
                    TstrItem(OrgIso73671DE.DOCUMENT_NUMBER_IDENTIFIER, false, null, null, null, null),
                    TstrItem(OrgIso73671DE.NUMBER_VRC_PART2_IDENTIFIER, true, null, null, null, Charset.LATIN1),
                    UintItem(OrgIso73671DE.OPERATING_PERIOD_START_IDENTIFIER, false, null, 1, 12),
                    UintItem(OrgIso73671DE.OPERATING_PERIOD_END_IDENTIFIER, false, null, 1, 12),
                    TdateItem(OrgIso73671DE.REGISTRATION_NUMBER_VALID_UNTIL_IDENTIFIER, false, null),
                    BooleanItem(OrgIso73671DE.REGISTRATION_ELECTRIC_IDENTIFIER, false, null),
                    BooleanItem(OrgIso73671DE.REGISTRATION_VINTAGE_IDENTIFIER, false, null),
                    CustomItem(OrgIso73671DE.BASIC_VEHICLE_INFO_NAT_IDENTIFIER, true, fun(data: CBORObject): Boolean {
                        try {
                            Cbor.decodeFromByteArray<BasicVehicleInfoNat>(data.EncodeToBytes())
                        } catch (e: Exception) {
                            return false
                        }
                        return true
                    }),
                    CustomItem(OrgIso73671DE.MASS_INFO_NAT_IDENTIFIER, true, fun(data: CBORObject): Boolean {
                        try {
                            Cbor.decodeFromByteArray<MassInfoNat>(data.EncodeToBytes())
                        } catch (e: Exception) {
                            return false
                        }
                        return true
                    }),
                    CustomItem(OrgIso73671DE.ENGINE_INFO_NAT_IDENTIFIER, true, fun(data: CBORObject): Boolean {
                        try {
                            Cbor.decodeFromByteArray<EngineInfoNat>(data.EncodeToBytes())
                        } catch (e: Exception) {
                            return false
                        }
                        return true
                    }),
                    CustomItem(OrgIso73671DE.ENVIRONMENT_INFO_NAT_IDENTIFIER, true, fun(data: CBORObject): Boolean {
                        try {
                            Cbor.decodeFromByteArray<EnvironmentInfoNat>(data.EncodeToBytes())
                        } catch (e: Exception) {
                            return false
                        }
                        return true
                    }),
                    CustomItem(OrgIso73671DE.AXIS_INFO_NAT_IDENTIFIER, true, fun(data: CBORObject): Boolean {
                        try {
                            Cbor.decodeFromByteArray<AxisInfoNat>(data.EncodeToBytes())
                        } catch (e: Exception) {
                            return false
                        }
                        return true
                    }),
                    CustomItem(OrgIso73671DE.DIMENSION_INFO_NAT_IDENTIFIER, true, fun(data: CBORObject): Boolean {
                        try {
                            Cbor.decodeFromByteArray<DimensionInfoNat>(data.EncodeToBytes())
                        } catch (e: Exception) {
                            return false
                        }
                        return true
                    }),
                    CustomItem(OrgIso73671DE.REGISTERED_USER_NAT_IDENTIFIER, false, fun(data: CBORObject): Boolean {
                        try {
                            Cbor.decodeFromByteArray<RegistrationHoldersNat>(data.EncodeToBytes())
                        } catch (e: Exception) {
                            return false
                        }
                        return true
                    }),
                )
            )
        )
    )
}