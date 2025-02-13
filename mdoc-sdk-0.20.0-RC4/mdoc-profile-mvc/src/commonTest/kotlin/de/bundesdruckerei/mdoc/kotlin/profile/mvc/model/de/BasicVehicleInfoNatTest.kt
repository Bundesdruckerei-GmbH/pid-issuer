package de.bundesdruckerei.mdoc.kotlin.profile.mvc.model.de

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.encodeToHexString
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BasicVehicleInfoNatTest {

    private val basicVehicleInfoNatHexString =
        "AA64747970656B364130324134434A4148356776617269616E747819313233343536373839303132333435363738393031323334356776657273696F6E78233132333435363738393031323334353637383930313233343536373839303132333435686B65795F626F64796442413131686B65795F74797065634141416C6D616E7566616374757265726BC8A853504552414E54C3896D617070726F76616C5F747970656141706D616E7566616374757265725F6B6579643030333972747970655F617070726F76616C5F64617465D903EC6A323032322D31302D3230736B65795F76617269616E745F76657273696F6E653030303030"

    @OptIn(ExperimentalSerializationApi::class, ExperimentalUnsignedTypes::class)
    @Test
    fun `verify LocalDate de-serialization for BasicVehicleInfoNat#typeApprovalDate`() {
        val expected = LocalDate.parse("2022-10-20")
        val basicVehicleInfoNat = Cbor {
            verifyValueTags = true
            verifyObjectTags = true
            verifyKeyTags = true
        }.decodeFromHexString<BasicVehicleInfoNat>(basicVehicleInfoNatHexString)

        assertEquals(expected, basicVehicleInfoNat.typeApprovalDate)

        val hexString = Cbor {
            useDefiniteLengthEncoding = true
            encodeValueTags = true
        }.encodeToHexString(BasicVehicleInfoNat.serializer(), basicVehicleInfoNat).uppercase()

        val actualFromHexString = Cbor {
            verifyValueTags = true
            verifyObjectTags = true
            verifyKeyTags = true
        }.decodeFromHexString<BasicVehicleInfoNat>(hexString).typeApprovalDate

        assertEquals(
            expected,
            actualFromHexString
        )
    }
}