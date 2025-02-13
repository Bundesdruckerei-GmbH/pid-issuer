/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.openid4vc.statuslist

import com.upokecenter.cbor.CBORObject

internal fun CBORObject.untagIf(tag: Int) =
    if (mostOuterTag.ToInt32Unchecked() == tag) {
        UntagOne()
    } else {
        this
    }
