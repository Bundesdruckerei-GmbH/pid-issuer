/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist

import COSE.OneKey
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.matchesPredicate
import assertk.assertions.startsWith
import com.fasterxml.jackson.annotation.JsonProperty
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jwt.SignedJWT
import com.upokecenter.cbor.CBORObject
import de.bdr.openid4vc.common.signing.Pkcs12Signer
import de.bdr.openid4vc.statuslist.StatusList
import de.bdr.openid4vc.statuslist.StatusListToken
import de.bdr.statuslist.service.TokenFormat
import de.bdr.statuslist.web.api.model.Reference
import de.bdr.statuslist.web.api.model.References
import de.bdr.statuslist.web.api.model.UpdateStatusRequest
import java.util.concurrent.TimeUnit
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.getForObject
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity

private const val BITS_PER_BYTE = 8

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.profiles.include=docker"],
)
class StatusListServiceApplicationIT(
    @Value("\${oid4vc.issuer.signer.pkcs12.keystore}") private val keystore: String,
    @Value("\${oid4vc.issuer.signer.pkcs12.password}") private val password: String,
    @Value("\${app.public-url}") private val publicUrl: String,
) {
    private val apiKey = "366A9069-2965-4667-9AD2-5C51D71046D8"
    private val verifier: ECDSAVerifier
    private val oneKey: OneKey

    init {
        val str = DefaultResourceLoader().getResource(keystore).inputStream
        val eck = Pkcs12Signer(str, password).keys.jwk.toECKey()
        this.verifier = ECDSAVerifier(eck)
        this.oneKey = OneKey(eck.toPublicKey(), null)
    }

    @Autowired lateinit var restTemplate: TestRestTemplate

    @Test fun contextLoads() {}

    @Nested
    inner class SingleBitList(
        @Value("\${app.status-list-pools.verified-email.issuer}") private val issuerUri: String
    ) {
        private val listIdentifier = "verified-email"
        private val listSize = 128
        private val bits = 1

        @Test
        fun `should get reference, get status-list and get aggregation`() {
            val reference = postForReference(listIdentifier)
            val list = getStatusList(reference, TokenFormat.JSON, ListUri::class.java)
            val aggregationUri = list.aggregationUri

            val aggregation = getAggregation(aggregationUri)

            with(aggregation) {
                assertThat(statusList).isNotEmpty()
                assertThat(statusList).contains(reference.uri)
            }
        }

        @Test
        fun `should get some references`() {
            val count = 10
            val headers = HttpHeaders()
            headers.set("x-api-key", apiKey)
            headers.accept = listOf(MediaType.APPLICATION_JSON)
            val referenceList =
                restTemplate.postForObject(
                    "/pools/$listIdentifier/new-references?amount=$count",
                    HttpEntity<References>(headers),
                    References::class.java,
                )

            assertThat(referenceList).isNotNull()
            assertThat(referenceList.references?.size).isEqualTo(count)
            (0..<count).forEach {
                assertThat(referenceList.references?.get(it)?.uri).isNotNull().startsWith(publicUrl)
                assertThat(referenceList.references?.get(it)?.index)
                    .isNotNull()
                    .isGreaterThanOrEqualTo(0)
            }
            assertThat(referenceList.references?.last()?.index)
                .isNotEqualTo(referenceList.references?.first()?.index)
        }

        @Test
        fun `should get valid distinct status-list as jwt`() {
            val reference = postForReference(listIdentifier)
            val listResponse = getStatusList(reference, TokenFormat.JWT, String::class.java)
            val token = StatusListToken.from(SignedJWT.parse(listResponse))

            assertThat(token.isNotExpired()).isTrue()
            assertThat(token.statusList.getList()).hasSize(listSize / BITS_PER_BYTE)
            assertThat(token.issuerUri).isEqualTo(issuerUri)
            assertThat(token.aggregationUri).isEqualTo("$publicUrl/aggregation/$listIdentifier")
            assertThat(token.jwt).isNotNull().matchesPredicate { it.verify(verifier) }
        }

        @Test
        fun `should get valid distinct status-list as cwt`() {
            val reference = postForReference(listIdentifier)
            val listResponse = getStatusList(reference, TokenFormat.CWT, ByteArray::class.java)
            val token = StatusListToken.from(CBORObject.DecodeFromBytes(listResponse))

            assertThat(token.isNotExpired()).isTrue()
            assertThat(token.statusList.getList()).hasSize(listSize / BITS_PER_BYTE)
            assertThat(token.issuerUri).isEqualTo(issuerUri)
            assertThat(token.aggregationUri).isEqualTo("$publicUrl/aggregation/$listIdentifier")
            assertThat(token.cwt).isNotNull().matchesPredicate { it.validate(oneKey) }
        }

        @Test
        fun `should get valid distinct status-list as json`() {
            val reference = postForReference(listIdentifier)
            val jsonList = getStatusList(reference, TokenFormat.JSON, ListUri::class.java)
            val statusList = StatusList.fromEncoded(jsonList.bits, jsonList.lst)

            assertThat(jsonList.bits).isEqualTo(bits)
            assertThat(jsonList.aggregationUri).isEqualTo("$publicUrl/aggregation/$listIdentifier")
            assertThat(statusList.getList()).hasSize(listSize / BITS_PER_BYTE)
        }

        @Test
        fun `should get valid distinct status-list as cbor`() {
            val reference = postForReference(listIdentifier)
            val listResponse = getStatusList(reference, TokenFormat.CBOR, ByteArray::class.java)
            val cborList = CBORObject.DecodeFromBytes(listResponse)
            val statusList = StatusList.fromCbor(cborList)

            assertThat(cborList.get("bits").AsInt32()).isEqualTo(bits)
            assertThat(cborList.get("aggregation_uri").AsString())
                .isEqualTo("$publicUrl/aggregation/$listIdentifier")
            assertThat(statusList.getList()).hasSize(listSize / BITS_PER_BYTE)
        }

        @Test
        @Disabled // TODO The default tamplate does not support PATCH calls, so convert the tests
        // for this into a separate task
        fun `should update status and poll status-list until new status is applied`() {
            val reference = postForReference(listIdentifier)
            val newStatus = 1
            updateStatus(reference, newStatus)

            await
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(java.time.Duration.ofSeconds(1))
                .untilAsserted {
                    val jsonList = getStatusList(reference, TokenFormat.JSON, ListUri::class.java)
                    val statusList = StatusList.fromEncoded(jsonList.bits, jsonList.lst)
                    val bitString =
                        statusList.getList()[reference.index / BITS_PER_BYTE].toBitString()
                    val indexBit = BITS_PER_BYTE - reference.index % BITS_PER_BYTE - 1

                    assertThat(bitString[indexBit]).isEqualTo(newStatus.digitToChar())
                }
        }
    }

    @Nested
    inner class MultiBitList {
        private val listIdentifier = "subscriptions-email"
        private val listSize = 128
        private val bits = 4

        @Test
        fun `should get valid distinct status-list as json`() {
            val reference = postForReference(listIdentifier)
            val jsonList = getStatusList(reference, TokenFormat.JSON, ListUri::class.java)
            val statusList = StatusList.fromEncoded(jsonList.bits, jsonList.lst)

            assertThat(jsonList.bits).isEqualTo(bits)
            assertThat(jsonList.aggregationUri).isEqualTo("$publicUrl/aggregation/$listIdentifier")
            assertThat(statusList.getList()).hasSize(listSize * bits / BITS_PER_BYTE)
        }

        @Test
        @Disabled // TODO The default tamplate does not support PATCH calls, so convert the tests
        // for this into a separate task
        fun `should update status and poll status-list until new status is applied`() {
            val reference = postForReference(listIdentifier)
            val newStatus = 5
            val newStatusBitString = "0101"
            updateStatus(reference, newStatus)

            await
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(java.time.Duration.ofSeconds(1))
                .untilAsserted {
                    val jsonList = getStatusList(reference, TokenFormat.JSON, ListUri::class.java)
                    val statusList = StatusList.fromEncoded(jsonList.bits, jsonList.lst)
                    val bitString =
                        statusList.getList()[reference.index * bits / BITS_PER_BYTE].toBitString()
                    val indexBit = BITS_PER_BYTE - reference.index * bits % BITS_PER_BYTE
                    val bitSubstring = bitString.substring(indexBit - bits, indexBit)

                    assertThat(bitSubstring).isEqualTo(newStatusBitString)
                }
        }
    }

    private fun postForReference(listIdentifier: String): Reference {
        val headers = HttpHeaders()
        headers.set("x-api-key", apiKey)
        val referenceResponse =
            restTemplate.postForObject(
                "/pools/$listIdentifier/new-references",
                HttpEntity<References>(headers),
                References::class.java,
            )

        assertThat(referenceResponse.references).isNotNull()
        with(referenceResponse.references!!.first()) {
            assertThat(uri).startsWith(publicUrl)
            assertThat(index).isGreaterThanOrEqualTo(0)
        }
        return referenceResponse.references!!.first()
    }

    private fun updateStatus(reference: Reference, value: Int) {
        val headers = HttpHeaders()
        headers.set("x-api-key", apiKey)
        val updateRequest = UpdateStatusRequest(reference.uri, reference.index, value)
        val updateResponse =
            restTemplate.patchForObject(
                "/status-lists/update",
                HttpEntity(updateRequest, headers),
                Void::class.java,
            )
        assertThat(updateResponse).isNotNull()
        //        assertThat(updateResponse.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    private fun <T> getStatusList(
        reference: Reference,
        tokenFormat: TokenFormat,
        responseFormat: Class<T>,
    ): T {
        val listUri = reference.uri.removePrefix(publicUrl)
        val entity = RequestEntity.get(listUri).accept(tokenFormat.mediaType).build()
        val listResponse = restTemplate.exchange(entity, responseFormat).body
        assertThat(listResponse).isNotNull()
        return listResponse ?: error("null response")
    }

    private fun getAggregation(aggregationUri: String): Aggregation {
        val aggregation =
            restTemplate.getForObject<Aggregation>(aggregationUri.removePrefix(publicUrl))
        assertThat(aggregation).isNotNull()
        return aggregation ?: error("null response")
    }
}

private fun Byte.toBitString(): String {
    return this.toUByte().toString(2).padStart(BITS_PER_BYTE, '0')
}

data class ListUri(
    val bits: Int,
    val lst: String,
    @JsonProperty("aggregation_uri") val aggregationUri: String,
)

data class Aggregation(@JsonProperty("status_lists") val statusList: List<String>)
