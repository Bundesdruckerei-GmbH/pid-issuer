/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.openid4vc.common

/**
 * Returns the current time in milliseconds. The default value is System::currentTimeMillis, but
 * ‚can be overridden for testing purposes.
 */
var currentTimeMillis: () -> Long = System::currentTimeMillis
