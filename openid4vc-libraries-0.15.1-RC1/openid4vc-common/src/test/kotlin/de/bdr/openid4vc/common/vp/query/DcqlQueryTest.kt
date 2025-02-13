/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp.query

import assertk.assertThat
import de.bdr.openid4vc.common.credentials.CredentialWithDisclosureSelection
import de.bdr.openid4vc.common.vp.CredentialQueryResolver
import de.bdr.openid4vc.common.vp.dcql.VerificationSettings
import java.lang.IllegalStateException
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource

/**
 * Tests DCQL queries.
 *
 * Implemented as a parameterized test, testcases are loaded from files as defined in
 * `test-cases.kt`.
 */
class DcqlQueryTest {

    companion object {
        @JvmStatic fun correctResultTestCases() = CorrectResultTestCase.all()

        @BeforeAll
        @JvmStatic
        fun `get the lazy properties from the test cases to detect errors early`() {
            DcqlQueryTestCase.entries.forEach {
                try {
                    it.queryCredentialsAndExpectedResult
                    it.correctResultTestCases
                } catch (e: Exception) {
                    throw IllegalStateException("Incorrect test case setup: $it", e)
                }
            }
        }
    }

    private val inTest = CredentialQueryResolver

    @ParameterizedTest
    @EnumSource(names = ["SD_JWT_WITH_CLAIMS_SETS_TWO_CREDENTIALS_WITH_DIFFERENT_MATCHES"])
    fun `when credentials are resolved the result is correct`(testcase: DcqlQueryTestCase) {
        val (query, credentials, expected) = testcase.queryCredentialsAndExpectedResult

        val result = inTest.resolve(credentials, query).toCredentialIdAndDisclosuresStructure()

        assertThat(result).isCorrectResolutionResult(expected)
    }

    @ParameterizedTest
    @MethodSource("correctResultTestCases")
    fun `when a correct result is verified the verification works`(
        testcase: CorrectResultTestCase
    ) {
        assertNotNull(
            inTest.verify(
                testcase.credentials,
                testcase.query,
                VerificationSettings(allowDisclosureOfUnnecessaryClaims = false),
            )
        )
    }

    private fun Collection<Map<String, CredentialWithDisclosureSelection>>
        .toCredentialIdAndDisclosuresStructure() =
        mapTo(mutableSetOf()) {
            it.mapValues { credentialWithDisclosures ->
                CredentialIdAndDisclosures(
                    (credentialWithDisclosures.value.credential as TestCredential).id,
                    credentialWithDisclosures.value.toDisclose,
                )
            }
        }
}
