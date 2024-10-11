/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bundesdruckerei.mdoc.kotlin.core.common

import kotlin.reflect.KClass

class CBORtoClassConversionException(targetClass: KClass<*>) :
    IllegalArgumentException("Cannot decode CBOR object to ${targetClass.simpleName}")
