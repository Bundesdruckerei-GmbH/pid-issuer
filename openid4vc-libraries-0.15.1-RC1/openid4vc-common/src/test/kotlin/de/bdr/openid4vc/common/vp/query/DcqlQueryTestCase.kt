/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common.vp.query

/**
 * Defines test cases for DCQL queries.
 *
 * Each testcase contains:
 * - a query
 * - a set of credentials in the wallet
 * - an expected result
 */
enum class DcqlQueryTestCase {

    /**
     * **Query** Query an SD-JWT VC by vct.
     *
     * **Credentials**:
     * - One credential with matching vct
     * - One credential with non-matching vct
     */
    SD_JWT_BY_VCT,

    /**
     * **Query** Query an SD-JWT VC with a claim.
     *
     * **Credentials**:
     * - One credential with the claim
     * - One credential without the claim
     */
    SD_JWT_WITH_CLAIM,

    /**
     * **Query** Query an SD-JWT VC with a claim.
     *
     * **Credentials**:
     * - One credential without the claim
     */
    SD_JWT_WITH_CLAIM_BUT_WITHOUT_MATCH,

    /**
     * **Query** Query an SD-JWT VC by string claim value.
     *
     * **Credentials**:
     * - One credential with matching claim value
     * - One credential with non-matching string claim value
     * - One credential with non-matching number claim value
     * - One credential with non-matching boolean claim value
     * - One credential with non-matching array claim value
     * - One credential with non-matching object claim value
     * - One credential with non-matching null claim value
     * - One credential without the claim
     */
    SD_JWT_BY_STRING_CLAIM,

    /**
     * **Query** Query an SD-JWT VC by claim value of type number.
     *
     * **Credentials**:
     * - One credential with matching claim value
     * - One credential with non-matching string claim value
     * - One credential with non-matching number claim value
     * - One credential with non-matching boolean claim value
     * - One credential with non-matching array claim value
     * - One credential with non-matching object claim value
     * - One credential with non-matching null claim value
     * - One credential without the claim
     */
    SD_JWT_BY_NUMBER_CLAIM,

    /**
     * **Query** Query an SD-JWT VC by claim value of type number.
     *
     * **Credentials**:
     * - One credential with matching claim value
     * - One credential with non-matching string claim value
     * - One credential with non-matching number claim value
     * - One credential with non-matching boolean claim value
     * - One credential with non-matching array claim value
     * - One credential with non-matching object claim value
     * - One credential with non-matching null claim value
     * - One credential without the claim
     */
    SD_JWT_BY_BOOLEAN_CLAIM,

    /**
     * **Query** Query an SD-JWT VC with a specific claim value.
     *
     * **Credentials**:
     * - One credential with a wrong value for the claim
     */
    SD_JWT_WITH_CLAIM_BUT_WITHOUT_VALUES_MATCH,

    /**
     * **Query** Query an SD-JWT VC and disclose an SD claim.
     *
     * **Credentials**:
     * - One credential with the claim as selective disclosure
     */
    SD_JWT_DISCLOSE_CLAIM,

    /**
     * **Query** Query an SD-JWT VC and disclose an SD claim.
     *
     * **Credentials**:
     * - One credential with the claim in a sublevel and a parent object as selective disclosure
     */
    SD_JWT_DISCLOSE_CLAIM_HIGHER_LEVEL,

    /**
     * **Query** Query two SD-JWT VCs with different claims.
     *
     * **Credentials**:
     * - One credential that matches the first query
     * - One credential that matches the second query
     * - A credential that does not match the queries
     */
    SD_JWT_MULTIPLE_CREDENTIAL_QUERIES,

    /**
     * **Query** Query an SD-JWT VC with a claim.
     *
     * **Credentials**:
     * - One credential that matches the query
     * - Another credential that matches the query
     * - A credential that does not match the query
     */
    SD_JWT_MULTIPLE_OPTIONS_SINGLE_CREDENTIAL_QUERY,

    /**
     * **Query** Query two SD-JWT VCs with different claims.
     *
     * **Credentials**:
     * - One credential that matches the first query
     * - Another credential that matches the first query
     * - One credential that matches the second query
     * - Another credential that matches the second query
     * - A credential that does not match the query
     */
    SD_JWT_MULTIPLE_OPTIONS_MULTIPLE_CREDENTIAL_QUERIES,

    /**
     * **Query** Query an SD-JWT VC with two specific claim values.
     *
     * **Credentials**:
     * - One credential with a wrong value for one and a correct value for the other the claim
     */
    SD_JWT_WITH_CLAIM_AND_ONE_VALUES_MATCH_BUT_ANOTHER_VALUES_MISMATCH,

    /**
     * **Query** Query an SD-JWT VC with a specific claim value in an array.
     *
     * **Credentials**:
     * - One credential with a wrong value and a correct value for the claim and SD array elements
     * - One credential with a wrong value and a correct value for the claim without SD array
     *   elements
     * - One credential with only a correct value for the claim
     * - One credential with only an incorrect value for the claim
     */
    SD_JWT_FILTER_ARRAY_BY_SINGLE_CLAIM,

    /**
     * **Query** Query an SD-JWT VC with two specific claim values in an array.
     *
     * **Credentials**:
     * - One credential with a correct values and no sd
     * - One credential with correct and wrong values and no sd
     * - One credential with correct and wrong values and sd on array element level
     * - One credential with correct and wrong values and sd on leaf claims
     */
    SD_JWT_FILTER_ARRAY_BY_SUBCLAIMS,

    /**
     * **Query** Query one SD-JWT VC A or two combined SD-JWT VCs B and C and one optional SD-JWT VC
     * D or SD-JWT VC E.
     *
     * **Credentials**:
     * - One credential A
     * - One credential B1
     * - One credential B2
     * - One credential C
     * - One credential D
     * - One credential E1
     * - One credential E2
     * - One non-matching credential
     */
    SD_JWT_WITH_CREDENTIAL_SETS,

    /**
     * **Query** Query an SD-JWT VC with two claims sets.
     *
     * **Credentials**
     * - One credentials that matches the first and second claims set
     */
    SD_JWT_WITH_CLAIMS_SETS_FIRST_AND_SECOND_MATCHES,

    /**
     * **Query** Query an SD-JWT VC with two claims sets.
     *
     * **Credentials**
     * - One credentials that matches the second claims set
     */
    SD_JWT_WITH_CLAIMS_SETS_SECOND_MATCHES,

    /**
     * **Query** Query an SD-JWT VC with two claims sets.
     *
     * **Credentials**
     * - One credentials that matches neither the first nor the second claims set
     */
    SD_JWT_WITH_CLAIMS_SETS_NO_MATCHES,

    /**
     * **Query** Query an SD-JWT VC with two claims sets.
     *
     * **Credentials**
     * - One credentials that matches the first claims set
     * - One credentials that matches the second claims set
     */
    SD_JWT_WITH_CLAIMS_SETS_TWO_CREDENTIALS_WITH_DIFFERENT_MATCHES,

    /**
     * **Query** Query an SD-JWT VC with two claims.
     *
     * **Credentials**
     * - A credential that contains both claims inside-of another object, the object and both of the
     *   claims are selectively discloseable. An additional claim that is not to be disclosed is
     *   there as well.
     */
    SD_JWT_WITH_NESTED_DISCLOSURES_AND_DISCLOSURE_OF_PARENT,

    /**
     * **Query** Query an SD-JWT VC with two claims.
     *
     * **Credentials**
     * - A credential that contains both claims inside-of another object, the object and one of the
     *   claims are selectively discloseable.
     */
    SD_JWT_WITH_NESTED_DISCLOSURES_AND_FULL_DISCLOSURE,

    /**
     * **Query** Query an SD-JWT VC with two specific array elements.
     *
     * **Credentials**
     * - A credential that contains both claims inside-of an array, the array and one of the claims
     *   are selectively discloseable. The non selectively discloseable claim has a non-matching
     *   value.
     */
    SD_JWT_WITH_NESTED_DISCLOSURES_WITH_DISCLOSURE_CONFLICT,

    /**
     * **Query** Query an SD-JWT VC with array elements of type object with two specific claims.
     *
     * **Credentials**
     * - A credential that contains
     * - - one array element, that matches all claims and is selectively discloseable
     * - - an element that matches only one claim and is selectively discloseable, the individual
     *   claims are not selectively discloseable
     * - - an element that matches no claims and is selectively discloseable, the individual claims
     *   are not selectively discloseable
     * - - an element that matches only one claim and is selectively discloseable, the individual
     *   claims are selectively discloseable
     * - - an element that matches no claims and is selectively discloseable, the individual claims
     *   are selectively discloseable
     */
    SD_JWT_WITH_NESTED_DISCLOSURES_WITH_DISCLOSURE_CONFLICT_BUT_ADDITIONAL_MATCHES

    //
    ;

    val queryCredentialsAndExpectedResult by lazy { loadQueryCredentialsAndExpectedResult(name) }

    val correctResultTestCases by lazy { loadCorrectResultTestCases(this) }
}
