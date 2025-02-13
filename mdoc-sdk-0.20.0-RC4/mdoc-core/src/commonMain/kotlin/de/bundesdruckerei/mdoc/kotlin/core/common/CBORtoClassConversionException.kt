/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bundesdruckerei.mdoc.kotlin.core.common

import kotlin.reflect.KClass

class CBORtoClassConversionException(targetClass: KClass<*>) :
    IllegalArgumentException("Cannot decode CBOR object to ${targetClass.simpleName}")
