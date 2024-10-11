/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci.proofs

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.nimbusds.jose.jwk.Curve.P_256
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException
import de.bdr.openid4vc.common.exceptions.SpecificIllegalArgumentException.ReasonCode.INVALID_PROOF
import de.bdr.openid4vc.common.formats.msomdoc.MsoMdocCredentialRequest
import de.bdr.openid4vc.common.formats.sdjwtvc.SdJwtVcCredentialRequest
import de.bdr.openid4vc.common.signing.JwkSigner
import de.bdr.openid4vc.common.vci.CredentialRequest
import de.bdr.openid4vc.common.vci.proofs.cwt.CwtProof
import de.bdr.openid4vc.common.vci.proofs.jwt.JwtProof
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProofsSerializationTest {

    companion object {
        val signer = JwkSigner(ECKeyGenerator(P_256).generate())

        val A_JWT_PROOF = JwtProof.create("clientId", "audience", "nonce1", signer)
        val ANOTHER_JWT_PROOF = JwtProof.create("clientId", "audience", "nonce1", signer)

        val A_CWT_PROOF = CwtProof.create("clientId", "audience", "nonce1", signer)
        val ANOTHER_CWT_PROOF = CwtProof.create("clientId", "audience", "nonce1", signer)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `given a json object without proof_type when deserialized as proof then MissingFieldException is thrown`() {
        assertThrows<MissingFieldException> { Json.decodeFromString<Proof>("{}") }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `given a json object with invalid proof_type when deserialized as proof then SerializationException is thrown`() {
        assertThrows<SerializationException> {
            Json.decodeFromString<Proof>("""{"proof_type": []}""")
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `given a json object with unsupported proof_type when deserialized as proof then SpecificIllegalArgumentException is thrown`() {
        val e =
            assertThrows<SpecificIllegalArgumentException> {
                Json.decodeFromString<Proof>("""{"proof_type": "invalid"}""")
            }
        assertThat(e.reason).isEqualTo(INVALID_PROOF)
    }

    @Test
    fun `given a list of jwt proofs when serialized and deserialized then the result equals the input`() {
        val request =
            SdJwtVcCredentialRequest(proofs = listOf(A_JWT_PROOF, ANOTHER_JWT_PROOF), vct = "Test")

        val encoded = Json.encodeToString(request)

        val deserialized = Json.decodeFromString<CredentialRequest>(encoded)

        assertThat(deserialized).isEqualTo(request)
    }

    @Test
    fun `given a serialized list of jwt proofs when deserialized it works`() {
        val encoded =
            """{"format":"vc+sd-jwt","vct":"Test","proofs":{"jwt":["eyJ0eXAiOiJvcGVuaWQ0dmNpLXByb29mK2p3dCIsImFsZyI6IkVTMjU2IiwiandrIjp7Imt0eSI6IkVDIiwiY3J2IjoiUC0yNTYiLCJ4IjoiZXZlcFZvM2JBUWhUVGg5aWhMaXRsdXREZm1lQ3BnVkN0dG1DSjZIMFctRSIsInkiOiJaMTBDalI1VGhURlNybVlQZmpqeWpycm01Z0l1TlAyMm56Vmt2Um12Y1dBIn19.eyJpc3MiOiJjbGllbnRJZCIsImF1ZCI6ImF1ZGllbmNlIiwibm9uY2UiOiJub25jZTEiLCJpYXQiOjE3MTg4ODkwMjl9.h7hgkocCt-AE7MNrTWr1CIcpUP6xCxrFKGPx8SSY9zGo3_K94PKjwBc6WXVh0RwclbhknbBINkGB4j6lX52HSQ","eyJ0eXAiOiJvcGVuaWQ0dmNpLXByb29mK2p3dCIsImFsZyI6IkVTMjU2IiwiandrIjp7Imt0eSI6IkVDIiwiY3J2IjoiUC0yNTYiLCJ4IjoiZXZlcFZvM2JBUWhUVGg5aWhMaXRsdXREZm1lQ3BnVkN0dG1DSjZIMFctRSIsInkiOiJaMTBDalI1VGhURlNybVlQZmpqeWpycm01Z0l1TlAyMm56Vmt2Um12Y1dBIn19.eyJpc3MiOiJjbGllbnRJZCIsImF1ZCI6ImF1ZGllbmNlIiwibm9uY2UiOiJub25jZTEiLCJpYXQiOjE3MTg4ODkwMjl9.uJYD2CwCZwy249vMEx51_nQdalqH4Nb22KKOLdMn6pvaBknps6EAlPURQe9Ztjhql47DuI4Lt0GC6sZ-FBVB1w"]}}"""

        Json.decodeFromString<CredentialRequest>(encoded)
    }

    @Test
    fun `given empty proofs when deserialized it throws SpecificIllegalArgumentException`() {
        val encoded = """{"format":"vc+sd-jwt","vct":"Test","proofs":{}}"""

        val e =
            assertThrows<SpecificIllegalArgumentException> {
                Json.decodeFromString<CredentialRequest>(encoded)
            }

        assertThat(e.reason).isEqualTo(INVALID_PROOF)
        assertThat(e.message).isEqualTo("Key proof list not present")
    }

    @Test
    fun `given a list of cwt proofs when serialized and deserialized then the result equals the input`() {
        val request =
            MsoMdocCredentialRequest(
                proofs = listOf(A_CWT_PROOF, ANOTHER_CWT_PROOF),
                doctype = "test"
            )

        val encoded = Json.encodeToString(request)

        val deserialized = Json.decodeFromString<CredentialRequest>(encoded)

        assertThat(deserialized).isEqualTo(request)
    }

    @Test
    fun `given a serialized list of cwt proofs when deserialized it works`() {
        val encoded =
            """{"format":"mso_mdoc","doctype":"test","proofs":{"cwt":["0oRYb6MBJgN0b3BlbmlkNHZjaS1wcm9vZitjd3RoQ09TRV9LZXlYS6QBAiABIVggB6NOfQ5OEwiWWfdq0FwetwzraVS55PP6D8sammQNffwiWCDuWtEW4gBtc3GBVmvDSiRQdin8B31yixhqlJ7-V5dNwKBYI6QBaGNsaWVudElkA2hhdWRpZW5jZQYaZnQqvApGbm9uY2UxWEA7QBaZD6Ak9ww_F8gvLaBWJ_zYIBuGtTT7M-0Hp88I8D6cxzJtHVRyBISimVGQSrvpopls_q7jUM2qlHGIA_OR","0oRYb6MBJgN0b3BlbmlkNHZjaS1wcm9vZitjd3RoQ09TRV9LZXlYS6QBAiABIVggB6NOfQ5OEwiWWfdq0FwetwzraVS55PP6D8sammQNffwiWCDuWtEW4gBtc3GBVmvDSiRQdin8B31yixhqlJ7-V5dNwKBYI6QBaGNsaWVudElkA2hhdWRpZW5jZQYaZnQqvApGbm9uY2UxWEAj0-bqAIS3BVq3En2T-UTl793PVFjWuD9Er5W7MxPFTbQ3lEDhLTybnuRFnodTWoMqaiZ_FIsaLl7FVUblD9_f"]}}"""

        Json.decodeFromString<CredentialRequest>(encoded)
    }
}
