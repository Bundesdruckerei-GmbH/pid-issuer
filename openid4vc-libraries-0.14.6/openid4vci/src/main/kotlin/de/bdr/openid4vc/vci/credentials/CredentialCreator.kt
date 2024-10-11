/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.vci.credentials

import com.nimbusds.jose.jwk.JWK
import de.bdr.openid4vc.common.signing.Signer
import de.bdr.openid4vc.common.vci.CredentialDescription
import de.bdr.openid4vc.common.vci.CredentialRequest
import de.bdr.openid4vc.vci.service.statuslist.StatusReference
import java.util.Locale
import java.util.UUID

/**
 * A factory and definition for credentials.
 *
 * Implementations must define
 * - the [CredentialConfiguration] to use and
 * - furtherOfferParameters used to build the credential offer.
 *
 * @see de.bdr.ssi.oid4vc.credentials.sdjwt.SdJwtVcCredentialCreator
 * @see de.bdr.ssi.oid4vc.credentials.mdoc.MDocCredentialCreator
 */
abstract class CredentialCreator {

    abstract val configuration: CredentialConfiguration

    abstract val signers: Collection<Signer>

    abstract fun getCredentialDescription(
        display: Map<Locale, Map<String, Any>>?
    ): CredentialDescription

    /**
     * Creates a credential.
     *
     * @param furtherRequestParameters Further request parameters that have been passed by the
     *   wallet. Before invocation the parameters are always validated using
     *   [validateCredentialRequest]. Thus, this method will only be invoked with valid parameters.
     * @param holderBindingKey A [JWK] to use as holder binding key
     * @param status A StatusReference pointing to a status list index to use
     * @return Credentials are always encoded as string. If the credential format does not produce a
     *   string, base64 should be used to produce a string from the raw bytes.
     */
    abstract fun create(
        request: CredentialRequest,
        issuanceId: UUID,
        holderBindingKey: JWK?,
        status: StatusReference?
    ): String

    open fun validateCredentialRequest(request: CredentialRequest) = true

    open fun onStatusListEntriesUsed(
        issuanceId: UUID,
        indicesByListUri: Map<String, Collection<Int>>
    ) {
        // to be overridden if needed
    }

    open fun getTransactionCode(issuanceId: UUID): TxCode? {
        return null
    }
}
