/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common.vci

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CredentialOffer(
    @SerialName("credential_issuer") val credentialIssuer: String,
    @SerialName("credential_configuration_ids") val credentialConfigurationIds: List<String>,
    @SerialName("grants") val grants: Grants? = null
)

@Serializable
data class Grants(
    @SerialName("urn:ietf:params:oauth:grant-type:pre-authorized_code")
    val preAuthorizedCodeGrant: PreAuthorizedCodeGrant? = null,
    @SerialName("authorization_code") val authorizationCodeGrant: AuthorizationCodeGrant? = null,
)

@Serializable
data class PreAuthorizedCodeGrant(
    /**
     * REQUIRED. The code representing the Credential Issuer's authorization for the Wallet to
     * obtain Credentials of a certain type. This code MUST be short lived and single use. If the
     * Wallet decides to use the Pre-Authorized Code Flow, this parameter value MUST be included in
     * the subsequent Token Request with the Pre-Authorized Code Flow.
     */
    @SerialName("pre-authorized_code") val preAuthorizedCode: String,
    /**
     * OPTIONAL. Object specifying whether the Authorization Server expects presentation of a
     * Transaction Code by the End-User along with the Token Request in a Pre-Authorized Code Flow.
     * If the Authorization Server does not expect a Transaction Code, this object is absent; this
     * is the default. The Transaction Code is intended to bind the Pre-Authorized Code to a certain
     * transaction to prevent replay of this code by an attacker that, for example, scanned the QR
     * code while standing behind the legitimate End-User. It is RECOMMENDED to send the Transaction
     * Code via a separate channel. If the Wallet decides to use the Pre-Authorized Code Flow, the
     * Transaction Code value MUST be sent in the tx_code parameter with the respective Token
     * Request as defined in Section
     * [6.1](https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#token-request)
     * . If no length or description is given, this object may be empty, indicating that a
     * Transaction Code is required,
     */
    @SerialName("tx_code") val txCodeMetadata: TxCodeMetadata? = null,
    /**
     * OPTIONAL string that the Wallet can use to identify the Authorization Server to use with this
     * grant type when authorization_servers parameter in the Credential Issuer metadata has
     * multiple entries. It MUST NOT be used otherwise. The value of this parameter MUST match with
     * one of the values in the authorization_servers array obtained from the Credential Issuer
     * metadata.
     */
    @SerialName("authorization_server") val authorizationServer: String? = null,
    /**
     * OPTIONAL. The minimum amount of time in seconds that the Wallet SHOULD wait between polling
     * requests to the token endpoint (in case the Authorization Server responds with error code
     * authorization_pending - see Section
     * [6.3](https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#token-error-response)
     * ). If no value is provided, Wallets MUST use 5 as the default.
     */
    @SerialName("interval") val interval: Int? = null,
)

@Serializable
data class AuthorizationCodeGrant(
    /**
     * OPTIONAL. String value created by the Credential Issuer and opaque to the Wallet that is used
     * to bind the subsequent Authorization Request with the Credential Issuer to a context set up
     * during previous steps. If the Wallet decides to use the Authorization Code Flow and received
     * a value for this parameter, it MUST include it in the subsequent Authorization Request to the
     * Credential Issuer as the issuer_state parameter value.
     */
    @SerialName("issuer_state") val issuerState: String? = null,
    /**
     * OPTIONAL string that the Wallet can use to identify the Authorization Server to use with this
     * grant type when authorization_servers parameter in the Credential Issuer metadata has
     * multiple entries. It MUST NOT be used otherwise. The value of this parameter MUST match with
     * one of the values in the authorization_servers array obtained from the Credential Issuer
     * metadata.
     */
    @SerialName("authorization_server") val authorizationServer: String? = null,
)

@Serializable
data class TxCodeMetadata(
    /**
     * OPTIONAL. String specifying the input character set. Possible values are numeric (only
     * digits) and text (any characters). The default is numeric
     */
    @SerialName("input_mode") @EncodeDefault val inputMode: InputMode = InputMode.NUMERIC,
    /**
     * OPTIONAL. Integer specifying the length of the Transaction Code. This helps the Wallet to
     * render the input screen and improve the user experience.
     */
    @SerialName("length") val length: Int? = null,
    /**
     * OPTIONAL. String containing guidance for the Holder of the Wallet on how to obtain the
     * Transaction Code, e.g., describing over which communication channel it is delivered. The
     * Wallet is RECOMMENDED to display this description next to the Transaction Code input screen
     * to improve the user experience. The length of the string MUST NOT exceed 300 characters. The
     * description does not support internationalization, however the Issuer MAY detect the Holder's
     * language by previous communication or an HTTP Accept-Language header within an HTTP GET
     * request for a Credential Offer URI.
     */
    @SerialName("description") val description: String? = null,
)

@Serializable
enum class InputMode {
    @SerialName("numeric") NUMERIC,
    @SerialName("text") TEXT,
}
