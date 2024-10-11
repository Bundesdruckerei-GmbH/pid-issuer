/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.vci.service.endpoints

class MissingNonceException(val nonce: String) : IllegalArgumentException("nonce value missing")
