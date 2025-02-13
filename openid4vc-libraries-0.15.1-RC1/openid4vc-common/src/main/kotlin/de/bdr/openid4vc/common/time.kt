/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.common

import java.time.Clock

/**
 * The clock used throughout the library to produce timestamps and compare timestamps with the
 * current time.
 *
 * This serves especially for testing purposes when time handling is tested, but can also serve as a
 * way to use a specific clock in the library.
 */
var clock: Clock = Clock.systemDefaultZone()

internal val currentTimeMillis: () -> Long = { clock.instant().toEpochMilli() }
